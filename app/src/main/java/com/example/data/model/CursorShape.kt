package com.example.data.model

enum class CursorShape {
    DEFAULT,      // Standard diagonal arrow
    POINTER,      // Interactive hand
    TEXT,         // I-beam text cursor
    WAIT,         // Spinning progress circle
    CROSSHAIR,    // Reticle/crosshair
    RESIZE_H,     // Horizontal resize arrows (<- ->)
    RESIZE_V,     // Vertical resize arrows
    GRAB,         // Open hand for drag initiation
    GRABBING,     // Closed hand during drag operation
    NOT_ALLOWED,  // Red circular slash for disabled elements
    NONE          // Invisible cursor
}
