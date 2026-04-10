package eu.kanade.domain.ui.model

  import dev.icerock.moko.resources.StringResource
  import tachiyomi.i18n.aniyomi.AYMR

  enum class DockStyle(val titleRes: StringResource) {
      NORMAL(titleRes = AYMR.strings.pref_dock_style_normal),
      FLOATING(titleRes = AYMR.strings.pref_dock_style_floating),
      FLOATING_CENTER(titleRes = AYMR.strings.pref_dock_style_floating_center),
      ;
  }
  