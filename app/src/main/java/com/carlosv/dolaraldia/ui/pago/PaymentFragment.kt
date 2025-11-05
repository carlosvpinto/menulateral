package com.carlosv.dolaraldia.ui.pago

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.carlosv.dolaraldia.utils.Constants
import com.carlosv.dolaraldia.utils.WebAppInterface
import com.carlosv.dolaraldia.utils.WebViewListener
import com.carlosv.menulateral.R
import com.carlosv.menulateral.databinding.FragmentPaymentBinding

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

class PaymentFragment : Fragment() {
    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!

    private var price : Float = 0f
    private val TAG = "PAYPALPRICE"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTexts()
        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        // Usa _binding de manera segura
        val binding = _binding ?: return  // Si _binding es null, termina la función

        binding.webView.apply {
            settings.javaScriptEnabled = true

            addJavascriptInterface(WebAppInterface(requireContext(), price, object : WebViewListener {
                override fun onSuccess() {
                    // Solo navega si el binding aún está disponible
                    _binding?.let {
                        findNavController().navigate(R.id.nav_home)
                    }
                }
            }), Constants.JS_ANDROID)

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    // Verifica si el binding aún es válido
                    _binding?.progressBar?.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Verifica si el binding aún es válido
                    _binding?.let {
                        it.progressBar.visibility = View.GONE

                        view?.evaluateJavascript("getStatus()") { statusResult ->
                            Log.i(TAG, "onPageFinished: $statusResult")
                        }

                        view?.loadUrl("javascript:(function(){ " +
                                "setPrice($price);" +
                                "})()")
                    }
                }
            }

            // Carga la URL solo si _binding aún es válido
            loadUrl(Constants.PAYPAL_URL)
        }
    }

    private fun setupTexts() {
        arguments?.let { args ->
            price = args.getFloat(Constants.ARG_PRICE, 0.0f)
            binding.tvPrice.text = getString(R.string.frgm_payment_price, price)
        }
    }

    private fun paymentFinished() {
        Toast.makeText(requireActivity(), R.string.frgm_payment_msg_thanks, Toast.LENGTH_SHORT).show()
        findNavController().navigate(R.id.nav_home)
    }



    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
