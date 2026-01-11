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
```

### Configure All Parameters at Once

You can also configure all parameters at once using `BorderDetectionConfig`:

```java
import com.davemorrissey.labs.subscaleview.decoder.BorderDetectionConfig;

BorderDetectionConfig config = new BorderDetectionConfig(
    1000,   // maxBorderDetectionDimension
    0.90,   // threshold
    0.10f   // filledRatioLimit
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

// Or using BorderDetectionConfig
val config = BorderDetectionConfig(
    maxBorderDetectionDimension = 1000,
    threshold = 0.90,
    filledRatioLimit = 0.10f
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

## Notes

- Border detection configuration must be set **before** loading the image
- Changes to border detection configuration will take effect for newly loaded images
- Border detection is only performed when `setCropBorders(true)` is enabled
- The default values work well for most scanned documents and images with letterbox borders
