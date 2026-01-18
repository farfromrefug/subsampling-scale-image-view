# Border Detection Configuration

This document describes how to configure the border detection parameters in SubsamplingScaleImageView.

## Overview

The library now supports configurable border detection parameters that were previously hardcoded:
- `MAX_BORDER_DETECTION_DIMENSION`: Controls the sampling resolution for border detection
- `THRESHOLD`: Controls the grayscale threshold for black/white detection
- `FILLED_RATIO_LIMIT`: Controls the sensitivity for detecting content vs. borders

## Basic Usage

### Enable Border Cropping

First, enable border cropping:

```java
SubsamplingScaleImageView imageView = findViewById(R.id.imageView);
imageView.setCropBorders(true);
```

### Configure Individual Parameters

You can configure each parameter individually using convenience methods:

```java
// Set max border detection dimension (default: 500)
// Higher values = more accurate detection but uses more memory
imageView.setMaxBorderDetectionDimension(1000);

// Set threshold (default: 0.95, range: 0.0 to 1.0)
// Pixels darker than (255 * threshold) are considered black
// Pixels lighter than (255 - 255 * threshold) are considered white
imageView.setBorderDetectionThreshold(0.90);

// Set filled ratio limit (default: 0.15, range: 0.0 to 1.0)
// Percentage of pixels that must be "filled" to detect content
// Lower values detect borders more aggressively
imageView.setBorderDetectionFilledRatioLimit(0.10f);

// Set crop only white (default: false)
// When true, only white borders are detected and cropped
imageView.setCropOnlyWhite(true);

// Set maximum crop percentage (default: null)
// Maximum percentage of width/height that can be cropped from any edge
// Prevents over-cropping of cover pages with centered content
imageView.setMaxCropPercentage(0.3f);  // 30% maximum per edge
```

### Configure All Parameters at Once

You can also configure all parameters at once using `BorderDetectionConfig`:

```java
import com.davemorrissey.labs.subscaleview.decoder.BorderDetectionConfig;

BorderDetectionConfig config = new BorderDetectionConfig(
    1000,   // maxBorderDetectionDimension
    0.90,   // threshold
    0.10f,  // filledRatioLimit
    true,   // cropOnlyWhite
    0.3f    // maxCropPercentage (Float, can be null)
);

imageView.setBorderDetectionConfig(config);
```

### Kotlin Example

```kotlin
import com.davemorrissey.labs.subscaleview.decoder.BorderDetectionConfig

val imageView: SubsamplingScaleImageView = findViewById(R.id.imageView)
imageView.cropBorders = true

// Using individual setters
imageView.setMaxBorderDetectionDimension(1000)
imageView.setBorderDetectionThreshold(0.90)
imageView.setBorderDetectionFilledRatioLimit(0.10f)
imageView.setCropOnlyWhite(true)
imageView.setMaxCropPercentage(0.3f)

// Or using BorderDetectionConfig
val config = BorderDetectionConfig(
    maxBorderDetectionDimension = 1000,
    threshold = 0.90,
    filledRatioLimit = 0.10f,
    cropOnlyWhite = true,
    maxCropPercentage = 0.3f
)
imageView.borderDetectionConfig = config
```

## Parameter Details

### maxBorderDetectionDimension

- **Default**: 500
- **Type**: int
- **Description**: Maximum dimension (width or height) for the sampled bitmap used in border detection. The actual image is downsampled to this resolution for border detection to improve performance.
- **Trade-offs**: 
  - Higher values: More accurate detection, uses more memory during detection
  - Lower values: Faster detection, uses less memory, may miss thin borders

### threshold

- **Default**: 0.95
- **Type**: double (0.0 to 1.0)
- **Description**: Controls the grayscale threshold for determining black and white pixels.
  - Pixels with value < (255 * threshold) are considered black
  - Pixels with value > (255 - 255 * threshold) are considered white
- **Trade-offs**:
  - Higher values (closer to 1.0): Only very dark/light pixels are considered borders
  - Lower values (closer to 0.0): More pixels are considered as borders

### filledRatioLimit

- **Default**: 0.15 (15%)
- **Type**: float (0.0 to 1.0)
- **Description**: The ratio of pixels that must be "filled" (non-border color) in a row or column to be considered content.
- **Trade-offs**:
  - Higher values: More conservative, requires more content pixels to crop borders
  - Lower values: More aggressive, crops borders with less content detected

### cropOnlyWhite

- **Default**: false
- **Type**: boolean
- **Description**: When true, only detects and crops white backgrounds. Black borders are ignored. This is useful for documents or scanned pages where you only want to remove white borders while preserving any black content or borders.
- **Use cases**:
  - Document scanning where pages have white margins but black text/borders
  - PDF pages with white borders but intentional black borders in the design
  - Images where black edges are part of the content

### maxCropPercentage

- **Default**: null (unlimited)
- **Type**: Float (0.0 to 1.0, nullable)
- **Description**: Maximum percentage of the original width or height that can be cropped from any single edge. If the detected border exceeds this percentage for any dimension (left, right, top, or bottom), no cropping is applied to that dimension. This prevents over-cropping on images like cover pages with centered text on white backgrounds.
- **Examples**:
  - `0.3f` = Maximum 30% can be cropped from any edge
  - `0.5f` = Maximum 50% can be cropped from any edge
  - `null` = No limit (default behavior)
- **Use cases**:
  - Preserving cover pages with minimal centered content
  - Preventing over-cropping when there's only a small amount of content
  - Protecting against false positives in border detection

## Examples

### Aggressive Border Cropping (for scanned documents with large borders)

```java
imageView.setCropBorders(true);
imageView.setBorderDetectionThreshold(0.98);  // Very strict black/white detection
imageView.setBorderDetectionFilledRatioLimit(0.05f);  // Aggressive cropping
```

### Conservative Border Cropping (for photos with natural black/white edges)

```java
imageView.setCropBorders(true);
imageView.setBorderDetectionThreshold(0.90);  // More lenient black/white detection
imageView.setBorderDetectionFilledRatioLimit(0.25f);  // Conservative cropping
```

### High Quality Detection (for large images)

```java
imageView.setCropBorders(true);
imageView.setMaxBorderDetectionDimension(2000);  // Higher resolution sampling
```

### White-Only Border Cropping (for documents)

```java
imageView.setCropBorders(true);
imageView.setCropOnlyWhite(true);  // Only crop white borders, preserve black borders
```

### Protect Cover Pages (prevent over-cropping centered content)

```java
imageView.setCropBorders(true);
imageView.setMaxCropPercentage(0.3f);  // Don't crop more than 30% from any edge
// If a cover page has centered text and would require cropping >30% from any edge,
// that dimension won't be cropped at all
```

### Complete Configuration for Document Processing

```java
// For a document viewer that needs to:
// - Only crop white margins
// - Prevent cropping cover pages with centered titles
// - Be aggressive about removing margins on regular pages
imageView.setCropBorders(true);
imageView.setCropOnlyWhite(true);              // Only white borders
imageView.setMaxCropPercentage(0.4f);          // Max 40% per edge
imageView.setBorderDetectionFilledRatioLimit(0.10f); // Aggressive on normal pages
```

## Notes

- Border detection configuration must be set **before** loading the image
- Changes to border detection configuration will take effect for newly loaded images
- Border detection is only performed when `setCropBorders(true)` is enabled
- The default values work well for most scanned documents and images with letterbox borders
- When `maxCropPercentage` is set, the limit is applied independently to each edge (left, right, top, bottom)
