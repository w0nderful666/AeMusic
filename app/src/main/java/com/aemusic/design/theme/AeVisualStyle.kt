package com.aemusic.design.theme

/**
 * Visual material strategy, independent from Light/Dark.
 *
 * MATERIAL is deliberately the runtime default so lower-end devices never pay for optional visual
 * effects.  GLASS is AeMusic's flagship visual direction.  A possible third style is intentionally
 * not represented until its visual language has been designed; placeholder enum values become API
 * debt surprisingly quickly.
 */
enum class AeVisualStyle {
    MATERIAL,
    GLASS,
}
