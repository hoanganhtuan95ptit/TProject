# Precompute Layout

Tài liệu kỹ thuật cho module `com.simple.phonetics.ui.precompute`.

---

## 1. Mục đích

Tách hoàn toàn việc **đo kích thước (measure)** và **bố cục (layout)** ra khỏi UI thread, để `onMeasure` và `onDraw` của View chỉ làm thao tác rẻ tiền: đọc số đã tính sẵn và phát lệnh vẽ.

Nguồn gốc: các custom view phức tạp (waveform, biểu đồ phonetics, card có nhiều text + icon) đang đo trên UI thread → janky khi scroll, khi data đổi, khi inflate hàng loạt.

---

## 2. Vấn đề & bối cảnh

Trên Android stock:
- `View.onMeasure()` chạy ở UI thread, bao gồm cả text measurement (đắt nhất).
- `wrap_content` lan ngược, mỗi lần dữ liệu đổi → measure lại cả cây.
- RecyclerView bind item → measure ngay tại frame hiển thị → drop frame.

Compose / Flutter cũng đo trên UI thread vì layout của họ phải xử lý case tổng quát (constraint phụ thuộc parent, sibling, animation từng frame).

**Trong app này**, ta có điều kiện thuận lợi:
- Biết trước width (full screen / list item full width).
- Data tĩnh trong nhiều giây/phút.
- View self-contained, không phụ thuộc sibling.

→ Có thể đo 1 lần ở background, cache kết quả, view chỉ vẽ.

---

## 3. Yêu cầu

### Chức năng
- Mô tả layout bằng data class (immutable).
- Hỗ trợ tối thiểu: **Text**, **Image**, **Linear** (horizontal/vertical, gap, padding, cross-align).
- Đo ở background, trả về spec đầy đủ (vị trí + kích thước + đối tượng vẽ sẵn).
- View chỉ giữ spec và vẽ.
- Dễ mở rộng spec mới (Border, Divider, Shape...) mà không sửa View hay engine.

### Phi chức năng
- **Thread-safe**: engine không đụng bất kỳ API UI thread nào (View, Context, Resources sau khi đã trích sẵn).
- **Zero allocation trong `onDraw`**: Paint, Rect, StaticLayout build sẵn trong spec.
- **Immutability**: spec sau khi hand-off sang UI là read-only.
- **Không phụ thuộc framework ngoài**: chỉ Android SDK + Kotlin coroutines (đã có sẵn trong project).

---

## 4. Kiến trúc

```
┌─────────────────┐    ┌──────────────────┐    ┌──────────────┐
│   LayoutNode    │───▶│   LayoutEngine   │───▶│   DrawSpec   │
│  (mô tả, data)  │    │  (đo, Dispatch.  │    │ (kết quả +   │
│                 │    │   Default)       │    │  cách vẽ)    │
└─────────────────┘    └──────────────────┘    └──────────────┘
        ▲                                              │
        │                                              ▼
   ViewModel /                                  ┌──────────────────┐
   Repository                                   │ PrecomputedView  │
                                                │  (chỉ vẽ)        │
                                                └──────────────────┘
        bất kỳ thread          bg thread              UI thread
```

| Tầng | Trách nhiệm | Thread |
|------|-------------|--------|
| `LayoutNode` | Mô tả cây layout (data thuần) | bất kỳ |
| `LayoutEngine.measure()` | Đo, gán vị trí, build StaticLayout / Rect | bg |
| `DrawSpec` (đa hình) | Giữ kết quả + tự biết `draw(canvas)` | hand-off |
| `PrecomputedView` | Báo size + uỷ thác vẽ cho spec | UI |

---

## 5. Các thành phần

### `LayoutNode.kt`
Sealed class mô tả input. Subtypes:
- `Text` — text, size, color, maxLines, typeface, line spacing, padding.
- `Image` — `Bitmap` đã load sẵn, width/height override (null = intrinsic), padding.
- `Linear` — orientation, children, gap, cross-align (START/CENTER/END), padding.

Phụ trợ: `Constraints(maxWidth, maxHeight)`, `EdgeInsets`, `Orientation`, `CrossAlign`.

**Quy tắc:** mọi resource phải resolve sẵn (`Bitmap` đã decode, color là `Int`, typeface đã load) — engine không được đụng `Context`.

### `LayoutEngine.kt`
Object pure-function: `measure(node, constraints) → DrawSpec`.

- Text: dùng `StaticLayout.Builder` với width = `constraint - padding`.
- Image: trả `ImageSpec` với `dst Rect` pre-built (offset bằng padding).
- Linear: 2-pass — pass 1 đo tất cả con để biết cross size, pass 2 gán vị trí với cross-align.

Không state. Gọi từ bất kỳ thread nào, lý tưởng là `Dispatchers.Default`.

### `DrawSpec.kt`
Abstract class — hợp đồng:
```kotlin
abstract class DrawSpec {
    abstract val left: Int; abstract val top: Int
    abstract val width: Int; abstract val height: Int
    fun draw(canvas: Canvas)                    // wrap save/translate/restore
    protected abstract fun onDrawContent(canvas: Canvas)
    abstract fun withPosition(l: Int, t: Int): DrawSpec
}
```

Subtypes hiện có:
- `TextSpec` — vẽ `StaticLayout.draw(canvas)`.
- `ImageSpec` — `canvas.drawBitmap(bmp, null, dst, sharedPaint)`.
- `GroupSpec` — đệ quy `children[i].draw(canvas)`.

Mở rộng: tạo class kế thừa `DrawSpec`, override 3 method, xong. Không cần sửa View.

### `PrecomputedView.kt`
~25 dòng:
```kotlin
var spec: DrawSpec? = null      // setter requestLayout + invalidate
override fun onMeasure(...) { setMeasuredDimension(spec?.width ?: 0, spec?.height ?: 0) }
override fun onDraw(canvas) { spec?.draw(canvas) }
```
Không biết Text/Image/Group, không đo, không coroutine.

### `Example.kt`
- `Example.cardNode(...)` build cây mẫu (icon + word + IPA).
- `CardViewModel` minh hoạ luồng: `viewModelScope.launch { measure trên Dispatchers.Default → emit StateFlow<DrawSpec?> }`.

---

## 6. Luồng hoạt động

```
1. Caller (ViewModel hoặc Repository) build LayoutNode (data thuần).
2. Biết width? (screenWidth từ DisplayMetrics, hoặc đo container 1 lần).
3. launch(Dispatchers.Default) { LayoutEngine.measure(node, Constraints(width)) }
4. Kết quả DrawSpec → emit qua StateFlow / LiveData / callback.
5. Fragment collect → gán view.spec = result trên UI thread.
6. View requestLayout → onMeasure đọc spec.width/height → onDraw gọi spec.draw().
```

Race condition: nếu data đổi liên tục, dùng `cancel + launch lại` hoặc `Flow.mapLatest` để chỉ giữ kết quả mới nhất.

---

## 7. Cách dùng

```kotlin
// Trong ViewModel
private val _spec = MutableStateFlow<DrawSpec?>(null)
val spec: StateFlow<DrawSpec?> = _spec

fun loadCard(word: String, ipa: String, widthPx: Int) {
    viewModelScope.launch {
        val node = Example.cardNode(appContext, R.drawable.ic_word, word, ipa)
        val result = withContext(Dispatchers.Default) {
            LayoutEngine.measure(node, Constraints(widthPx))
        }
        _spec.value = result
    }
}

// Trong Fragment
viewLifecycleOwner.lifecycleScope.launch {
    viewModel.spec.collect { precomputedView.spec = it }
}
```

---

## 8. Hạn chế & khi nào KHÔNG dùng

- Caller phải **biết width trước**. Nếu width đến từ parent động (vd nested trong ConstraintLayout co giãn), pattern này không hợp.
- Không hỗ trợ **wrap-content lan ngược**: View báo `(0, 0)` khi chưa có spec.
- Không hỗ trợ animation per-frame (kích thước đổi liên tục → đo lại liên tục → đắt).
- Hiện chưa có: **weight/flex**, **wrap line**, **background/border drawable**, **click hit-test**.
- View nhỏ và đơn giản → overhead bg + sync còn đắt hơn measure thẳng. Chỉ dùng khi measure thực sự đắt (nhiều text, nhiều primitive).

---

## 9. Hướng mở rộng (chưa làm)

| Tính năng | Hướng làm |
|-----------|----------|
| Spec mới (Border, Divider, Path...) | Tạo class kế thừa `DrawSpec`, override `onDrawContent` + `withPosition`. |
| Weight trong Linear | Thêm `weight: Float` vào `LayoutNode`, engine pass 2: chia phần dư theo weight. |
| Wrap (auto xuống dòng) | `LayoutNode.Flow` — engine break khi cursor + childWidth > maxWidth. |
| Cache | `LruCache<Key, DrawSpec>` với `Key = hash(node, width, fontScale, density)`. |
| Hit-test (click) | Walk cây spec, check `(x, y)` rơi vào spec nào — gán `id` lên `LayoutNode` để map ngược. |
| Background/border | `LayoutNode.Box(child, background, border, radius)` → `BorderSpec(child)`. |
| Pre-measure ở Repository | Đo ngay khi data về từ DB, cache vào memory → bind view 0ms. |

---

## 10. Tham chiếu

- `PrecomputedText` / `PrecomputedTextCompat` (AndroidX): pattern tương tự cho text.
- Compose `Measurable / Placeable`: tách rõ measure → place → draw.
- Flutter `RenderObject.performLayout()` vs `paint()`.
- Skia `DisplayList`: cùng tư tưởng — record lệnh vẽ trước, replay sau.
