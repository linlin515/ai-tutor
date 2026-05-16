package com.aitutor.app.ui.camera

/**
 * Represents a detected text region from ML Kit on-device OCR.
 *
 * Bounding box coordinates are in the raw (sensor) image coordinate space.
 * Transform to display/canvas coordinates using rotationDegrees and canvas dimensions
 * when drawing the overlay.
 *
 * @property text The recognized text string.
 * @property left Left edge in raw image coordinate space.
 * @property top Top edge in raw image coordinate space.
 * @property right Right edge in raw image coordinate space.
 * @property bottom Bottom edge in raw image coordinate space.
 */
data class OcrBoundingBox(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}
