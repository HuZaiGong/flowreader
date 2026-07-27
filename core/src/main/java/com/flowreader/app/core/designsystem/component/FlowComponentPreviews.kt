package com.flowreader.app.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * The four renders every design-system component must survive.
 *
 * Light and dark catch hard-coded colors; `fontScale = 1.5` catches fixed-height rows that clip at
 * accessibility text sizes; the Arabic locale catches padding written as `start`/`end`-blind
 * absolute left/right. Anything added to this package without these previews is unreviewed.
 */
@Preview(name = "浅色", showBackground = true, widthDp = 360)
@Preview(name = "深色", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "大字号", showBackground = true, widthDp = 360, fontScale = 1.5f)
@Preview(name = "RTL", showBackground = true, widthDp = 360, locale = "ar")
annotation class FlowComponentPreviews
