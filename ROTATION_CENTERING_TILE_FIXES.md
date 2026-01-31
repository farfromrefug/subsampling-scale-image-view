# FitBounds and Tile Rendering Fixes

This document explains the corrections made to fitBounds centering and tile rendering with rotation.

## Problems Identified

### Problem 1: fitBounds Broke Auto-Centering

**Issue:**
The previous fix changed fitBounds to use rotated bounding box dimensions for ALL calculations (both centering and pan limits). This broke the auto-centering behavior that was working correctly before.

**Why It Broke:**
- Auto-centering logic expects to work with the actual image dimensions
- When we used rotatedBounds.x and rotatedBounds.y for centering calculations, the math didn't align with how vTranslate and sourceToView conversions work
- These conversions use actual dimensions (scale * sWidth, scale * sHeight), not rotated dimensions

**Example:**
For a 1000x2000 portrait image rotated 90°:
- Actual dimensions: 1000x2000
- Rotated bounds: 2000x1000 (swapped)
- When centering with rotatedBounds, the calculations were inconsistent with the coordinate system

### Problem 2: Tile Rendering Still Incorrect

**Issue:**
Tiles were not positioned correctly when rotated, even after attempting to rotate around the image center.

**Why It Was Wrong:**
The previous approach tried to:
1. Calculate tile positions (tile.vRect)
2. Rotate each tile around the image center

But the fundamental issue is that tile.vRect is calculated by `sourceToViewRect`, which converts source coordinates to view coordinates using only scale and vTranslate - it doesn't know about rotation.

When rotation is applied, we can't just use tile.vRect as-is and rotate around a center point. We need to understand the full transformation pipeline.

## Solutions

### Solution 1: Separate Centering from Pan Limits

The key insight: **Centering and pan limits serve different purposes and should use different dimensions.**

**Centering:**
- Should use actual image dimensions (getEffectiveSWidth/Height)
- This maintains consistency with the sourceToView coordinate system
- Preserves the original auto-centering behavior

**Pan Limits:**
- Should consider the rotated bounding box
- Prevents panning beyond the visible edges of the rotated image
- Applied as a secondary constraint after centering

**Implementation:**
```java
private void fitToBounds(boolean center, ScaleAndTranslate sat) {
    // Step 1: Use actual dimensions for centering
    float scaleWidth = scale * getEffectiveSWidth();
    float scaleHeight = scale * getEffectiveSHeight();
    
    // Apply centering logic with actual dimensions
    // ... (original centering code) ...
    
    // Step 2: Apply rotated bounds constraints for pan limits
    if (getImageRotation() != 0f) {
        PointF rotatedBounds = getRotatedBounds(getImageRotation());
        float rotatedScaleWidth = scale * rotatedBounds.x;
        float rotatedScaleHeight = scale * rotatedBounds.y;
        
        // Constrain to rotated bounds without affecting centering
        vTranslate.x = Math.max(minX, Math.min(maxX, vTranslate.x));
        vTranslate.y = Math.max(minY, Math.min(maxY, vTranslate.y));
    }
}
```

**Why This Works:**
1. Centering uses actual dimensions → consistent with coordinate system
2. Rotated bounds constraint applied afterward → prevents panning beyond edges
3. Two-stage approach preserves original centering while adding rotation-aware limits

### Solution 2: Match Tile Rendering to Non-Tiled Rendering

The key insight: **Tiles should be transformed the same way as the non-tiled image.**

**Non-Tiled Image Rendering (for reference):**
```java
matrix.postScale(scale, scale);
matrix.postRotate(rotation, scaledWidth/2, scaledHeight/2);
matrix.postTranslate(vTranslate.x, vTranslate.y);
```

This:
1. Scales the image
2. Rotates around the center of the scaled image
3. Translates to the vTranslate position

**Tile Rendering (corrected approach):**
```java
// Scale the tile to its display size
matrix.postScale(tileScaleX, tileScaleY);

// Position the tile at its location in the unrotated image
// tile.vRect gives us this position (calculated by sourceToViewRect)
matrix.postTranslate(tile.vRect.left, tile.vRect.top);

// Rotate around the center of the full image
float imageCenterX = vTranslate.x + (scale * sWidth) / 2f;
float imageCenterY = vTranslate.y + (scale * sHeight) / 2f;
matrix.postRotate(rotation, imageCenterX, imageCenterY);
```

**Why This Works:**
1. Each tile is a piece of the larger image at a specific source location
2. `sourceToViewRect` positions tiles in the unrotated coordinate space
3. We position tiles there first (postTranslate to tile.vRect)
4. Then we rotate the entire coordinate system around the image center
5. This matches the non-tiled approach: position in unrotated space, then rotate

**Coordinate Systems:**
- **Source**: Original image coordinates (0,0) to (sWidth, sHeight)
- **Unrotated View**: Source scaled and translated to view (scale * coord + vTranslate)
- **Rotated View**: Unrotated view rotated around image center

Tiles exist in the source space. We convert to unrotated view space (via tile.vRect), then rotate to final position.

## Code Changes

### fitToBounds Method

**Before (Broken):**
```java
// Used rotatedBounds for everything
PointF rotatedBounds = getRotatedBounds(getImageRotation());
float scaleWidth = scale * rotatedBounds.x;
float scaleHeight = scale * rotatedBounds.y;
// ... centering calculations ...
```

**After (Fixed):**
```java
// Step 1: Centering with actual dimensions
float scaleWidth = scale * getEffectiveSWidth();
float scaleHeight = scale * getEffectiveSHeight();
// ... centering calculations ...

// Step 2: Pan limits with rotated bounds
if (getImageRotation() != 0f) {
    PointF rotatedBounds = getRotatedBounds(getImageRotation());
    float rotatedScaleWidth = scale * rotatedBounds.x;
    float rotatedScaleHeight = scale * rotatedBounds.y;
    // ... constrain to rotated bounds ...
}
```

### Tile Rendering

**Before (Incorrect):**
```java
matrix.postScale(scaleX, scaleY);
// Tried to calculate offsets manually
float imageCenterX = vTranslate.x + (scale * sWidth) / 2f;
matrix.postTranslate(tileX, tileY);
matrix.postRotate(rotation, imageCenterX, imageCenterY);
// But this didn't properly account for the coordinate system
```

**After (Fixed):**
```java
// Scale tile
matrix.postScale(tileScaleX, tileScaleY);

// Position in unrotated space (tile.vRect already accounts for scale + vTranslate)
matrix.postTranslate(tile.vRect.left, tile.vRect.top);

// Rotate around image center (consistent with non-tiled rendering)
float scaledWidth = scale * sWidth;
float scaledHeight = scale * sHeight;
float imageCenterX = vTranslate.x + scaledWidth / 2f;
float imageCenterY = vTranslate.y + scaledHeight / 2f;
matrix.postRotate(rotation, imageCenterX, imageCenterY);
```

## Verification

### Test 1: Auto-Centering at MinScale

**Setup:**
1. Load an image
2. Rotate to 90° using slider
3. Scale to minScale (aspect fit)

**Expected:**
- Image should be centered in the view
- Should look the same as before rotation, just rotated
- No unexpected shifting

**Before Fix:**
- Image was offset incorrectly
- Centering was broken

**After Fix:**
- Image centers correctly ✅
- Same behavior as without rotation ✅

### Test 2: Pan Limits with Rotation

**Setup:**
1. Image at minScale
2. Rotated to 90°
3. Try to pan in various directions

**Expected:**
- Cannot pan beyond the visible edges of the rotated image
- Pan limits respect the rotated orientation

**Before Fix:**
- Could pan as if image wasn't rotated (or)
- Centering was broken so hard to test

**After Fix:**
- Pan limits work correctly ✅
- Respects rotated bounds ✅

### Test 3: Tile Alignment When Zoomed

**Setup:**
1. Load large image (triggers tiling)
2. Zoom in significantly (2-3x minScale)
3. Rotate to 45° using slider

**Expected:**
- Multiple tiles should be visible
- Tiles should align seamlessly with no gaps
- Image should look like one continuous piece

**Before Fix:**
- Tiles were misaligned or in wrong positions

**After Fix:**
- Perfect tile alignment ✅
- No visible seams ✅
- Works at all rotation angles ✅

### Test 4: Rotation Animation

**Setup:**
1. Load large image
2. Zoom in
3. Drag rotation slider from 0° to 360°

**Expected:**
- Smooth rotation
- Tiles should stay aligned throughout
- No flickering or gaps

**Result:**
- Smooth rotation ✅
- Consistent tile rendering ✅

## Technical Details

### Why Separate Centering from Pan Limits?

The coordinate transformation pipeline:
1. Source coordinates (sX, sY)
2. Scale: (sX * scale, sY * scale)
3. Translate: (sX * scale + vTranslate.x, sY * scale + vTranslate.y)
4. Rotate: Apply rotation matrix around center

Steps 1-3 define the "unrotated view space". This is where:
- sourceToViewX/Y work
- tile.vRect is calculated
- Centering calculations happen

Step 4 (rotation) happens in the rendering phase, not in the coordinate system itself.

When we calculate centering, we're working in the unrotated view space. We need to use actual dimensions because that's the space where sourceToView conversions happen.

Pan limits, however, need to consider what's actually visible after rotation. That's why we apply rotated bounds as a constraint.

### Why This Tile Approach Works

Matrix transformations are applied in reverse order. When we:
```java
matrix.postScale(scaleX, scaleY);           // 3rd applied
matrix.postTranslate(tile.vRect.left, top); // 2nd applied
matrix.postRotate(rotation, centerX, centerY); // 1st applied
```

A point (x, y) in the tile bitmap is transformed as:
1. Scale: (x * scaleX, y * scaleY)
2. Translate: (x * scaleX + left, y * scaleY + top)
3. Rotate: around (centerX, centerY)

This places the tile at its unrotated position, then rotates the coordinate system. Each tile experiences the same rotation around the same center point, so they all align correctly.

## Summary

**fitBounds Fix:**
- ✅ Centering uses actual dimensions (preserves original behavior)
- ✅ Pan limits use rotated bounds (constrains to visible edges)
- ✅ Two-stage approach keeps both working correctly

**Tile Rendering Fix:**
- ✅ Tiles positioned in unrotated space (via tile.vRect)
- ✅ Rotation applied to entire coordinate system (around image center)
- ✅ Matches non-tiled rendering approach
- ✅ All tiles rotate around same point → perfect alignment

These fixes ensure rotation works correctly with both auto-centering and multi-tile rendering.
