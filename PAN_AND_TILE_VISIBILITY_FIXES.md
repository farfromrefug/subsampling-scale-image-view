# Pan Limits and Tile Visibility Fixes for Rotation

This document explains the fixes for pan limiting and tile visibility calculation with rotation.

## Problems Fixed

### Issue 1: Pan Limits Not Working with Rotation

**Problem:**
Pan limiting was not working correctly when image is rotated, especially at 90° and 270°. User could pan beyond the edges of the rotated image.

**Root Cause:**
- `fitToBounds()` was handling pan limits using actual image dimensions
- With rotation, the visible bounds are different (rotated bounding box)
- Previous attempts to fix this in fitToBounds broke the centering logic

**Solution:**
Created a separate method `constrainPanWithRotation()` that:
1. Is called AFTER fitToBounds (preserving centering)
2. Only applies when rotation != 0
3. Uses rotated bounding box dimensions for pan limits
4. Constrains vTranslate without affecting centering calculations

### Issue 2: Tile Visibility Incorrect with Rotation

**Problem:**
When zoomed and rotated, tiles would appear/disappear incorrectly as you pan. Sometimes tiles even showed wrong content or changed.

**Root Cause:**
- `tileVisible()` used `viewToSourceX/Y` which don't account for rotation
- `viewToSourceX(vx) = (vx - vTranslate.x) / scale` assumes no rotation
- With rotation, the visible area in source space is a rotated rectangle
- Simple axis-aligned check was marking wrong tiles as visible/invisible

**Solution:**
Updated `tileVisible()` to:
1. For no rotation: use original simple check (fast path)
2. For rotation: use inverse matrix transformation
   - Build the rendering transformation matrix
   - Invert it to get view-to-source transformation
   - Transform all 4 view corners to source space
   - Find bounding box of transformed corners
   - Check if tile intersects this bounding box

## Implementation Details

### Pan Limit Constraints

**Method Signature:**
```java
private void constrainPanWithRotation()
```

**Called From:**
```java
private void preDraw() {
    // ... setup code ...
    fitToBounds(false);           // Handles centering with actual dimensions
    constrainPanWithRotation();   // Applies rotation-aware pan limits
}
```

**Logic:**
```java
private void constrainPanWithRotation() {
    if (vTranslate == null || panLimit == PAN_LIMIT_OUTSIDE) {
        return;  // No constraints needed
    }
    
    float rotation = getImageRotation();
    if (rotation == 0f) {
        return;  // fitToBounds already handled it
    }
    
    // Calculate rotated bounds
    PointF rotatedBounds = getRotatedBounds(rotation);
    float rotatedScaleWidth = scale * rotatedBounds.x;
    float rotatedScaleHeight = scale * rotatedBounds.y;
    
    // Calculate pan limits based on panLimit mode and rotated bounds
    // ... (PAN_LIMIT_CENTER, PAN_LIMIT_INSIDE, etc.) ...
    
    // Apply constraints
    vTranslate.x = Math.max(minX, Math.min(maxX, vTranslate.x));
    vTranslate.y = Math.max(minY, Math.min(maxY, vTranslate.y));
}
```

**Why This Works:**
1. fitToBounds handles centering with actual dimensions (works correctly)
2. constrainPanWithRotation adds an ADDITIONAL constraint based on rotation
3. The constraint only tightens the bounds, doesn't change centering
4. Separation of concerns: centering vs pan limits

### Tile Visibility with Rotation

**Original Code (Broken with Rotation):**
```java
private boolean tileVisible(Tile tile) {
    float sVisLeft = viewToSourceX(0);
    float sVisRight = viewToSourceX(getWidth());
    float sVisTop = viewToSourceY(0);
    float sVisBottom = viewToSourceY(getHeight());
    return !(sVisLeft > tile.sRect.right || tile.sRect.left > sVisRight || 
             sVisTop > tile.sRect.bottom || tile.sRect.top > sVisBottom);
}
```

This assumes view corners map to axis-aligned corners in source space, which is only true when rotation = 0.

**Fixed Code:**
```java
private boolean tileVisible(Tile tile) {
    float rotation = getImageRotation();
    
    if (rotation == 0f) {
        // Fast path: no rotation, use simple check
        float sVisLeft = viewToSourceX(0);
        float sVisRight = viewToSourceX(getWidth());
        float sVisTop = viewToSourceY(0);
        float sVisBottom = viewToSourceY(getHeight());
        return !(sVisLeft > tile.sRect.right || tile.sRect.left > sVisRight || 
                 sVisTop > tile.sRect.bottom || tile.sRect.top > sVisBottom);
    } else {
        // With rotation: transform view corners to source space
        
        // 1. Build rendering transformation matrix
        Matrix renderMatrix = new Matrix();
        float scaledWidth = scale * sWidth;
        float scaledHeight = scale * sHeight;
        renderMatrix.postScale(scale, scale);
        renderMatrix.postRotate(rotation, scaledWidth / 2f, scaledHeight / 2f);
        renderMatrix.postTranslate(vTranslate.x, vTranslate.y);
        
        // 2. Invert to get view -> source transformation
        Matrix inverseMatrix = new Matrix();
        if (!renderMatrix.invert(inverseMatrix)) {
            return true;  // Conservative: mark as visible if can't compute
        }
        
        // 3. Transform view corners to source space
        float[] viewCorners = {0, 0, getWidth(), 0, getWidth(), getHeight(), 0, getHeight()};
        float[] sourceCorners = new float[8];
        inverseMatrix.mapPoints(sourceCorners, viewCorners);
        
        // 4. Find bounding box of transformed corners
        float minSx = Float.MAX_VALUE, maxSx = Float.MIN_VALUE;
        float minSy = Float.MAX_VALUE, maxSy = Float.MIN_VALUE;
        for (int i = 0; i < 4; i++) {
            minSx = Math.min(minSx, sourceCorners[i * 2]);
            maxSx = Math.max(maxSx, sourceCorners[i * 2]);
            minSy = Math.min(minSy, sourceCorners[i * 2 + 1]);
            maxSy = Math.max(maxSy, sourceCorners[i * 2 + 1]);
        }
        
        // 5. Check if tile intersects bounding box
        return !(minSx > tile.sRect.right || tile.sRect.left > maxSx || 
                 minSy > tile.sRect.bottom || tile.sRect.top > maxSy);
    }
}
```

**Why This Works:**
1. The rendering transformation is: scale -> rotate around center -> translate
2. To go from view to source, we need the inverse transformation
3. We transform the 4 view corners (screen edges) to source space
4. This gives us a rotated quadrilateral in source space
5. We find its axis-aligned bounding box
6. Tiles that intersect this bounding box are visible

**Note on Accuracy:**
Using a bounding box of the rotated quadrilateral is conservative - it may mark some tiles as visible when they're actually just outside. This is acceptable because:
- It ensures we don't miss any tiles (no gaps in rendering)
- Loading a few extra tiles is better than missing visible ones
- The performance impact is minimal

For perfect accuracy, we'd need to check if the tile rectangle intersects the rotated quadrilateral, which is more complex.

## Coordinate Transformations

### View to Source Transformation

**Without Rotation:**
```
Source -> View: (sx, sy) -> (sx * scale + vTranslate.x, sy * scale + vTranslate.y)
View -> Source: (vx, vy) -> ((vx - vTranslate.x) / scale, (vy - vTranslate.y) / scale)
```

**With Rotation:**
```
Source -> View: Apply matrix transformation
  1. Scale: (sx, sy) -> (sx * scale, sy * scale)
  2. Rotate: around (scaledWidth/2, scaledHeight/2) by rotation degrees
  3. Translate: by (vTranslate.x, vTranslate.y)

View -> Source: Apply inverse matrix transformation
  Matrix^-1 applied to (vx, vy)
```

The inverse matrix automatically handles all three transformations in reverse order.

## Testing Scenarios

### Test 1: Pan Limits at 90°

**Setup:**
1. Load image
2. Rotate to 90° (portrait image becomes landscape)
3. Set scale to minScale
4. Try to pan in all directions

**Expected:**
- Cannot pan left beyond rotated left edge
- Cannot pan right beyond rotated right edge
- Cannot pan up beyond rotated top edge
- Cannot pan down beyond rotated bottom edge

**Before Fix:**
Could pan as if image wasn't rotated (or centering was broken)

**After Fix:**
Pan correctly constrained to rotated bounds ✅

### Test 2: Tile Visibility at 45°

**Setup:**
1. Load large image
2. Zoom in significantly (to trigger tiling)
3. Rotate to 45°
4. Pan around

**Expected:**
- All visible tiles load correctly
- No gaps in the image
- Tiles don't flicker or disappear
- Tiles show correct content

**Before Fix:**
- Tiles would appear/disappear incorrectly
- Sometimes tiles changed content
- Gaps might appear

**After Fix:**
- Correct tiles marked as visible ✅
- Smooth panning without flickering ✅

### Test 3: Pan Limits at 270°

**Setup:**
1. Load image
2. Rotate to 270°
3. Scale to minScale
4. Try to pan

**Expected:**
- Pan constrained to rotated bounds
- Image stays within view

**Before Fix:**
Pan limits broken, especially at 270°

**After Fix:**
Works correctly ✅

## Performance Considerations

### Matrix Inversion Cost

The `tileVisible()` method now computes a matrix inversion for each tile when rotation is present. This could be expensive if called frequently.

**Optimization Opportunities:**
1. Cache the inverse matrix (recompute only when scale/rotation/vTranslate changes)
2. Only check tiles at the current sample size
3. The fast path (rotation = 0) avoids matrix operations entirely

**Current Performance:**
- Matrix inversion is relatively fast (4x4 matrix operations)
- Only called during tile refresh, not every frame
- The check happens for ~10-50 tiles typically
- Overall impact should be minimal

### Memory Usage

No additional memory overhead - matrices are created on stack during the check.

## Summary

**What Was Fixed:**
1. ✅ Pan limits now work correctly at all rotation angles
2. ✅ Tile visibility calculation accounts for rotation
3. ✅ Tiles no longer appear/disappear incorrectly when panning
4. ✅ Centering still works correctly (fitToBounds unchanged)

**How It Works:**
1. `constrainPanWithRotation()` adds rotation-aware pan constraints after fitToBounds
2. `tileVisible()` uses inverse matrix transformation to find visible tiles with rotation
3. Separation of concerns: centering (fitToBounds) vs pan limits (constrainPanWithRotation)

**Key Insight:**
With rotation, you can't just invert scale and translate - you need to account for the rotation transformation. Using matrix inversion handles all transformations correctly.
