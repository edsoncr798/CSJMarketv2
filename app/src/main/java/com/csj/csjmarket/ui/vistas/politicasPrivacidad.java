package com.csj.csjmarket.ui.vistas;

import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.csj.csjmarket.R;
import com.csj.csjmarket.databinding.FragmentPedidosBinding;
import com.csj.csjmarket.databinding.FragmentPoliticasPrivacidadBinding;


public class politicasPrivacidad extends Fragment {
    private FragmentPoliticasPrivacidadBinding binding;
    private View view;
    private AlertDialog alertDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentPoliticasPrivacidadBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        binding.webPoliticas.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                mostrarLoader();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                alertDialog.dismiss();
            }
        });

        binding.webPoliticas.loadUrl("https://www.comsanjuan.com/Privacidad/PoliticaPrivacidad.html");

        return view;
    }

    private void mostrarLoader() {
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        View progress = getLayoutInflater().inflate(R.layout.loader, null);
        builder.setView(progress);
        builder.setCancelable(false);
        alertDialog = builder.create();
        alertDialog.show();
    }
}