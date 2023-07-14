package org.thoughtcrime.securesms.components.settings.app.usernamelinks.main

import android.os.Bundle
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CoroutineScope
import org.signal.core.ui.Buttons
import org.signal.core.ui.Dialogs
import org.signal.core.ui.theme.SignalTheme
import org.signal.core.util.concurrent.LifecycleDisposable
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.settings.app.usernamelinks.main.UsernameLinkSettingsState.ActiveTab
import org.thoughtcrime.securesms.compose.ComposeFragment

@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalPermissionsApi::class
)
class UsernameLinkSettingsFragment : ComposeFragment() {

  private val viewModel: UsernameLinkSettingsViewModel by viewModels()
  private val disposables: LifecycleDisposable = LifecycleDisposable()

  @Composable
  override fun FragmentContent() {
    val state by viewModel.state
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val scope: CoroutineScope = rememberCoroutineScope()
    val navController: NavController by remember { mutableStateOf(findNavController()) }

    Scaffold(
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = { TopAppBarContent(state.activeTab) }
    ) { contentPadding ->

      if (state.indeterminateProgress) {
        Dialogs.IndeterminateProgressDialog()
      }

      AnimatedVisibility(
        visible = state.activeTab == ActiveTab.Code,
        enter = slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }),
        exit = slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth })
      ) {
        UsernameLinkShareScreen(
          state = state,
          snackbarHostState = snackbarHostState,
          scope = scope,
          modifier = Modifier.padding(contentPadding),
          navController = navController
        )
      }

      AnimatedVisibility(
        visible = state.activeTab == ActiveTab.Scan,
        enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }),
        exit = slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth })
      ) {
        UsernameQrScanScreen(
          lifecycleOwner = viewLifecycleOwner,
          disposables = disposables.disposables,
          qrScanResult = state.qrScanResult,
          onQrCodeScanned = { data -> viewModel.onQrCodeScanned(data) },
          onQrResultHandled = { viewModel.onQrResultHandled() },
          modifier = Modifier.padding(contentPadding)
        )
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    disposables.bindTo(viewLifecycleOwner)
  }

  override fun onResume() {
    super.onResume()
    viewModel.onResume()
  }

  @Composable
  private fun TopAppBarContent(activeTab: ActiveTab) {
    val cameraPermissionState: PermissionState = rememberPermissionState(permission = android.Manifest.permission.CAMERA)

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(64.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .fillMaxHeight(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        TabButton(
          label = stringResource(R.string.UsernameLinkSettings_code_tab_name),
          active = activeTab == ActiveTab.Code,
          onClick = { viewModel.onTabSelected(ActiveTab.Code) },
          modifier = Modifier.padding(end = 8.dp)
        )
        TabButton(
          label = stringResource(R.string.UsernameLinkSettings_scan_tab_name),
          active = activeTab == ActiveTab.Scan,
          onClick = {
            if (cameraPermissionState.status.isGranted) {
              viewModel.onTabSelected(ActiveTab.Scan)
            } else {
              cameraPermissionState.launchPermissionRequest()
            }
          }
        )
      }
    }
  }

  @Composable
  private fun TabButton(label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = if (active) {
      ButtonDefaults.filledTonalButtonColors()
    } else {
      ButtonDefaults.buttonColors(
        containerColor = SignalTheme.colors.colorSurface2,
        contentColor = MaterialTheme.colorScheme.onSurface
      )
    }
    Buttons.MediumTonal(
      onClick = onClick,
      modifier = modifier.defaultMinSize(minWidth = 100.dp),
      shape = RoundedCornerShape(12.dp),
      colors = colors
    ) {
      Text(label)
    }
  }

  @Preview
  @Composable
  private fun AppBarPreview() {
    SignalTheme(isDarkMode = false) {
      Surface {
        TopAppBarContent(activeTab = ActiveTab.Code)
      }
    }
  }

  @Preview
  @Composable
  fun PreviewAll() {
    FragmentContent()
  }
}
