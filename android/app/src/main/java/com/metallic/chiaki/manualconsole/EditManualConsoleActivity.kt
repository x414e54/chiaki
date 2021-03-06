// SPDX-License-Identifier: LicenseRef-GPL-3.0-or-later-OpenSSL

package com.metallic.chiaki.manualconsole

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.metallic.chiaki.R
import com.metallic.chiaki.common.RegisteredHost
import com.metallic.chiaki.common.ext.RevealActivity
import com.metallic.chiaki.common.ext.viewModelFactory
import com.metallic.chiaki.common.getDatabase
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_edit_manual.*

class EditManualConsoleActivity: AppCompatActivity(), RevealActivity
{
	companion object
	{
		const val EXTRA_MANUAL_HOST_ID = "manual_host_id"
	}

	override val revealIntent: Intent get() = intent
	override val revealRootLayout: View get() = rootLayout
	override val revealWindow: Window get() = window

	private lateinit var viewModel: EditManualConsoleViewModel

	private val disposable = CompositeDisposable()

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_edit_manual)
		handleReveal()

		viewModel = ViewModelProvider(this, viewModelFactory {
				EditManualConsoleViewModel(getDatabase(this),
					if(intent.hasExtra(EXTRA_MANUAL_HOST_ID))
						intent.getLongExtra(EXTRA_MANUAL_HOST_ID, 0)
					else
						null)
			})
			.get(EditManualConsoleViewModel::class.java)

		viewModel.existingHost?.observe(this, Observer {
			hostEditText.setText(it.host)
		})

		viewModel.selectedRegisteredHost.observe(this, Observer {
			registeredHostTextView.setText(titleForRegisteredHost(it))
		})

		viewModel.registeredHosts.observe(this, Observer { hosts ->
			registeredHostTextView.setAdapter(ArrayAdapter<String>(this, R.layout.dropdown_menu_popup_item,
				hosts.map { titleForRegisteredHost(it) }))
			registeredHostTextView.onItemClickListener = object: AdapterView.OnItemClickListener {
				override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
				{
					if(position >= hosts.size)
						return
					val host = hosts[position]
					viewModel.selectedRegisteredHost.value = host
				}
			}
		})


		saveButton.setOnClickListener { saveHost() }
	}

	private fun titleForRegisteredHost(registeredHost: RegisteredHost?) =
		if(registeredHost == null)
			getString(R.string.add_manual_regist_on_connect)
		else
			"${registeredHost.ps4Nickname ?: ""} (${registeredHost.ps4Mac})"

	private fun saveHost()
	{
		val host = hostEditText.text.toString().trim()
		if(host.isEmpty())
		{
			hostEditText.error = getString(R.string.entered_host_invalid)
			return
		}

		saveButton.isEnabled = false
		viewModel.saveHost(host)
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe {
				finish()
			}
			.addTo(disposable)
	}
}