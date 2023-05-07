package com.termux.gui.views

import android.content.Context
import android.widget.ProgressBar

/**
 * Progress bar that sets the style to horizontal in the constructor.
 */
class HorizontalProgressBar(c: Context) : ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal)
