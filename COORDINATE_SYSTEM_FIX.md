# Coordinate System Fix for Rotation

This document explains the correct approach to handling rotation with tiles and centering.

## Problem Analysis

The user reported two issues:
1. **Centering broken**: "centering at min scale is changed" - only want pan limit to change
2. **Tile positioning wrong**: "tiles are not rendered at the correct place when rotated"

Additionally, the user mentioned: "i made some fixes and now minscale works for all rotations perfectly". This indicates that minScale calculation already accounts for rotation correctly.

## Root Causes

### Issue 1: fitToBounds Was Modified

**What Happened:**
Previous fixes tried to use rotated bounding box dimensions in fitToBounds for both centering AND pan limits.

**Why It Was Wrong:**
- fitToBounds handles TWO distinct things: centering and pan constraints
- Centering logic expects to work with actual image dimensions
- The coordinate system (scale, vTranslate) is based on actual dimensions, not rotated bounds
- Changing dimensions in fitToBounds broke the centering calculations

**Correct Approach:**
- Don't modify fitToBounds at all
- It was working correctly before
- If pan limits need adjustment, that should be done separately

### Issue 2: Tile Coordinate System Confusion

**What Happened:**
Tile rendering tried to use tile.vRect positions directly, then rotate.

**Why It Was Wrong:**
- tile.vRect is calculated by `sourceToViewRect(tile.sRect, tile.vRect)`
- `sourceToViewRect` uses: `(int) sourceToViewX(sRect.left)` where `sourceToViewX(sx) = sx * scale + vTranslate.x`
- So tile.vRect ALREADY includes vTranslate!
- When we positioned tiles at tile.vRect.left then applied rotation around imageCenterX = vTranslate.x + ..., we were double-applying vTranslate

**Correct Approach:**
- Tiles should follow the exact same transformation as the non-tiled image
- Use tile.sRect (source coordinates) not tile.vRect (view coordinates with vTranslate)
- Apply transformations in the same order as non-tiled rendering

## Solutions

### Solution 1: Revert fitToBounds

**Code Change:**
Removed all rotation-aware modifications from fitToBounds. Restored to original:

```java
private void fitToBounds(boolean center, ScaleAndTranslate sat) {
    // Original code unchanged
    float scaleWidth = scale * getEffectiveSWidth();
    float scaleHeight = scale * getEffectiveSHeight();
    // ... original centering and pan limit logic ...
}
```

**Why This Works:**
- Preserves the working centering behavior
- Centering calculations use actual dimensions consistently with coordinate system
- minScale() already accounts for rotation (user's fix), so centering works correctly

**Future Work:**
If pan limits need to account for rotation, this should be added as a SEPARATE constraint AFTER fitToBounds, not within it.

### Solution 2: Fix Tile Coordinate System

**Transformation Order (Non-Tiled Image):**
```java
// Non-tiled image rendering
matrix.postScale(scale, scale);                          // 1. Scale
matrix.postRotate(rotation, scaledWidth/2, scaledHeight/2); // 2. Rotate around center
matrix.postTranslate(vTranslate.x, vTranslate.y);        // 3. Translate
```

**Transformation Order (Tiles) - FIXED:**
```java
// Tile rendering - should match non-tiled exactly
matrix.postScale(tileScaleX, tileScaleY);           // 1. Scale tile
matrix.postTranslate(tile.sRect.left * scale,       // 2. Position at source location
                     tile.sRect.top * scale);        //    (NOT tile.vRect!)
matrix.postRotate(rotation, scaledWidth/2,          // 3. Rotate around full image center
                           scaledHeight/2);
matrix.postTranslate(vTranslate.x, vTranslate.y);   // 4. Translate (same as non-tiled)
```

**Key Points:**
1. Use `tile.sRect` (source coordinates 0 to sWidth/sHeight) not `tile.vRect`
2. Convert source to scaled: `tile.sRect.left * scale`
3. Rotate around the same center point as non-tiled: `(scaledWidth/2, scaledHeight/2)`
4. Apply same vTranslate as non-tiled

**Why This Works:**
- Each tile is positioned at its correct location in the unrotated scaled image
- All tiles rotate around the same center point (the full image center)
- vTranslate is applied once at the end, moving the entire rotated image
- This matches exactly how the non-tiled image is rendered

## Coordinate Systems Explained

### Three Coordinate Spaces

1. **Source Space**: Original image coordinates
   - Range: (0, 0) to (sWidth, sHeight)
   - This is where tile.sRect is defined

2. **Scaled Space**: After applying scale
   - Range: (0, 0) to (scale * sWidth, scale * sHeight)
   - Position: source * scale
   - This is the "unrotated view" space

3. **View Space**: After applying scale, rotation, and vTranslate
   - Final position on screen
   - Position: rotate(source * scale) + vTranslate
   - This is where pixels are actually drawn

### Why tile.vRect Was Confusing

`tile.vRect` is calculated as:
```java
sourceToViewRect(tile.sRect, tile.vRect);

// Which does:
tile.vRect.left = (int) sourceToViewX(tile.sRect.left);
// where sourceToViewX(sx) = sx * scale + vTranslate.x
```

So `tile.vRect` is already in View Space (includes vTranslate).

When we tried to use tile.vRect.left as a position and then add vTranslate, we were in the wrong coordinate space and double-applying vTranslate.

The fix: Use tile.sRect (Source Space), convert to Scaled Space, then apply rotation and vTranslate.

## Tile Visibility Calculation

**Current Code:**
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

**Does It Work With Rotation?**
- `viewToSourceX(vx) = (vx - vTranslate.x) / scale`
- This converts view coordinates to source coordinates
- It does NOT account for rotation
- With rotation, the visible area in source coordinates is NOT a simple rectangle

**Potential Issue:**
When rotation is applied, the visible area on screen corresponds to a rotated rectangle in source space. The current visibility check treats it as an axis-aligned rectangle, which could:
1. Mark some tiles as visible when they're not (waste memory/loading)
2. Mark some tiles as not visible when they are (gaps in rendering)

**TODO:**
Need to investigate if tile visibility calculation needs to account for rotation. This might require:
- Converting the four corners of the view rectangle through the inverse transformation
- Checking if the tile rectangle intersects the rotated quadrilateral
- Or a more conservative approach: expand the visibility bounds when rotation is present

## Summary

**What Was Fixed:**
1. ✅ Reverted fitToBounds to preserve working centering
2. ✅ Fixed tile rendering to use correct coordinate system

**What Works Now:**
1. ✅ Centering at minScale (preserved original behavior)
2. ✅ Tile positioning with rotation (matches non-tiled rendering)

**What May Still Need Work:**
1. ⚠️ Pan limits with rotation (if needed, add separately from fitToBounds)
2. ⚠️ Tile visibility calculation with rotation (may need adjustment)

**Key Takeaway:**
The coordinate system matters! Always use the correct space:
- tile.sRect → Source Space → needs scale, rotation, vTranslate
- tile.vRect → View Space → already includes scale and vTranslate

Match the transformation order of non-tiled rendering for consistency.
