# Float Rotation and ColorFilter Support

This document describes the new features added to SubsamplingScaleImageView for float-based rotation and ColorFilter support.

## Float-Based Rotation

### Overview
The `imageRotation` property now supports arbitrary float values (in degrees) instead of just 90-degree increments. This enables smooth rotation animations.

### Usage

#### Setting a Specific Rotation Angle
```java
SubsamplingScaleImageView imageView = findViewById(R.id.imageView);

// Set rotation to any angle (in degrees)
imageView.setImageRotation(45.0f);  // 45 degrees
imageView.setImageRotation(123.5f); // 123.5 degrees
```

#### Animating Rotation
```java
// Using ObjectAnimator for smooth rotation animation
ObjectAnimator rotationAnimator = ObjectAnimator.ofFloat(
    imageView, 
    "imageRotation", 
    0f,    // Start angle
    360f   // End angle
);
rotationAnimator.setDuration(2000); // 2 seconds
rotationAnimator.start();
```

#### Backward Compatibility
The original enum-based API is still supported:
```java
// Using ImageRotation enum (backward compatible)
imageView.setImageRotation(ImageRotation.ROTATION_90);
imageView.setImageRotation(ImageRotation.ROTATION_180);
```

### Getting Current Rotation
```java
float currentRotation = imageView.getImageRotation();
```

### Implementation Notes

1. **Exact 90-degree rotations** (0°, 90°, 180°, 270°) use the optimized poly-to-poly transformation for maximum precision.

2. **Arbitrary angles** use matrix transformations with proper scaling and positioning.

3. **Effective dimensions** are automatically calculated for arbitrary rotation angles using trigonometry to determine the bounding box size.

4. **Tile loading** for arbitrary angles uses the source rectangles as-is, with rotation applied during rendering.

## ColorFilter Support

### Overview
SubsamplingScaleImageView now supports ColorFilter, similar to the standard Android ImageView. This allows you to apply color effects like tinting, color matrices, and blend modes to the displayed image.

### Usage

#### Setting a Color Filter
```java
SubsamplingScaleImageView imageView = findViewById(R.id.imageView);

// Apply a simple color tint
ColorFilter tintFilter = new PorterDuffColorFilter(
    Color.RED, 
    PorterDuff.Mode.MULTIPLY
);
imageView.setColorFilter(tintFilter);
```

#### Using ColorMatrix
```java
// Create a grayscale effect
ColorMatrix matrix = new ColorMatrix();
matrix.setSaturation(0); // 0 = grayscale, 1 = original colors

ColorMatrixColorFilter grayscaleFilter = new ColorMatrixColorFilter(matrix);
imageView.setColorFilter(grayscaleFilter);
```

#### Sepia Effect
```java
// Create a sepia effect
ColorMatrix sepiaMatrix = new ColorMatrix(new float[]{
    0.393f, 0.769f, 0.189f, 0, 0,
    0.349f, 0.686f, 0.168f, 0, 0,
    0.272f, 0.534f, 0.131f, 0, 0,
    0,      0,      0,      1, 0
});

imageView.setColorFilter(new ColorMatrixColorFilter(sepiaMatrix));
```

#### Removing a Color Filter
```java
imageView.setColorFilter(null);
```

#### Getting Current Color Filter
```java
ColorFilter currentFilter = imageView.getColorFilter();
```

### Implementation Notes

1. The ColorFilter is applied to the internal `bitmapPaint` used for both tiled and non-tiled rendering.

2. The filter is automatically applied when the paint object is created, ensuring consistent behavior.

3. Calling `setColorFilter()` triggers an `invalidate()`, causing the view to redraw with the new filter.

## Examples

### Example 1: Smooth Rotation with Color Tint
```java
SubsamplingScaleImageView imageView = findViewById(R.id.imageView);
imageView.setImage(ImageSource.uri("/path/to/image.jpg"));

// Apply a blue tint
imageView.setColorFilter(new PorterDuffColorFilter(
    Color.argb(100, 0, 0, 255),
    PorterDuff.Mode.SRC_ATOP
));

// Animate rotation
ObjectAnimator.ofFloat(imageView, "imageRotation", 0f, 360f)
    .setDuration(3000)
    .start();
```

### Example 2: Interactive Rotation Control
```java
SeekBar rotationSeekBar = findViewById(R.id.rotationSeekBar);
rotationSeekBar.setMax(360);
rotationSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        imageView.setImageRotation((float) progress);
    }
    
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}
    
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
});
```

### Example 3: Night Mode Filter
```java
// Create a night mode effect by reducing brightness and adding blue tint
ColorMatrix brightnessMatrix = new ColorMatrix();
float brightness = -0.3f; // Reduce brightness
brightnessMatrix.set(new float[] {
    1, 0, 0, 0, brightness * 255,
    0, 1, 0, 0, brightness * 255,
    0, 0, 1, 0, brightness * 255,
    0, 0, 0, 1, 0
});

imageView.setColorFilter(new ColorMatrixColorFilter(brightnessMatrix));
```

## Performance Considerations

1. **Rotation Animation**: For smooth animations, use hardware acceleration and consider the image size. Very large images may experience performance degradation during rotation.

2. **Arbitrary Angles**: Rotation at arbitrary angles may be slightly less performant than exact 90-degree rotations due to the additional matrix calculations.

3. **ColorFilter**: ColorFilter operations are applied at render time and are generally efficient, but complex filters may impact performance on lower-end devices.

4. **Tiled Rendering**: Both rotation and ColorFilter work seamlessly with tiled rendering for large images, maintaining memory efficiency.

## Compatibility

- **Minimum SDK**: No change from the base library requirements
- **Breaking Changes**: None - the enum-based rotation API remains available for backward compatibility
- **New API Level Requirements**: None - uses standard Android graphics APIs

## Migration Guide

### From Enum to Float Rotation

If you were using the ImageRotation enum:

**Before:**
```java
imageView.setImageRotation(ImageRotation.ROTATION_90);
```

**After (for animation support):**
```java
imageView.setImageRotation(90.0f);
```

**After (keeping enum for static rotations):**
```java
// Still works! No changes needed for backward compatibility
imageView.setImageRotation(ImageRotation.ROTATION_90);
```

## Known Limitations

1. **Tile loading for arbitrary angles**: When using non-90-degree rotations, tiles are loaded in their original orientation and rotated during rendering. This is optimal for most use cases but may not be ideal for very specific scenarios.

2. **Debug visualization**: Debug rect drawing around tiles doesn't account for arbitrary rotations.

## Future Enhancements

Potential future improvements could include:
- Optimized tile loading for arbitrary rotation angles
- Animated transitions between different rotation angles with interpolation
- Additional color filter presets
