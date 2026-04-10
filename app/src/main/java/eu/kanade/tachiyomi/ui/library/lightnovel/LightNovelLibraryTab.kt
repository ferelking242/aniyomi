package eu.kanade.tachiyomi.ui.library.lightnovel

  import androidx.compose.material.icons.Icons
  import androidx.compose.material.icons.outlined.AutoStories
  import androidx.compose.runtime.Composable
  import androidx.compose.ui.graphics.vector.rememberVectorPainter
  import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
  import cafe.adriel.voyager.navigator.tab.TabOptions
  import eu.kanade.domain.ui.model.NavStyle
  import eu.kanade.presentation.util.Tab
  import eu.kanade.tachiyomi.ui.library.manga.MangaLibraryTab
  import tachiyomi.i18n.aniyomi.AYMR
  import tachiyomi.presentation.core.i18n.stringResource
  import cafe.adriel.voyager.navigator.Navigator

  data object LightNovelLibraryTab : Tab {

      override val options: TabOptions
          @Composable
          get() {
              val isSelected = LocalTabNavigator.current.current.key == key
              val index: UShort = 6u
              return TabOptions(
                  index = index,
                  title = stringResource(AYMR.strings.label_ln_library),
                  icon = rememberVectorPainter(Icons.Outlined.AutoStories),
              )
          }

      override suspend fun onReselect(navigator: Navigator) {
          MangaLibraryTab.onReselect(navigator)
      }

      @Composable
      override fun Content() {
          MangaLibraryTab.Content()
      }
  }
  