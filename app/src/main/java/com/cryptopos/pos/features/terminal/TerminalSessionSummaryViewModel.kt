package com.cryptopos.pos.features.terminal

import androidx.lifecycle.ViewModel
import com.cryptopos.pos.domain.terminal.TerminalSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TerminalSessionSummaryViewModel @Inject constructor(
    val sessionManager: TerminalSessionManager,
) : ViewModel()
