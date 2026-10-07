/*
 * Copyright (C) 2026 MaxxOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.utils

import android.app.WallpaperManager
import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.util.AttributeSet
import android.widget.ImageView

import com.android.settings.R

class WallpaperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    private var blurred = false

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.WallpaperView, 0, 0).apply {
            try {
                blurred = getBoolean(R.styleable.WallpaperView_blurred, false)
            } finally {
                recycle()
            }
        }
        scaleType = ScaleType.CENTER_CROP
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        try {
            val wm = WallpaperManager.getInstance(context)
            wm.drawable?.let { setImageDrawable(it) }
            if (blurred) {
                setRenderEffect(RenderEffect.createBlurEffect(25f, 25f, Shader.TileMode.CLAMP))
            }
        } catch (e: Exception) {
            // Fallback: leave any static preview in place
        }
    }
}
