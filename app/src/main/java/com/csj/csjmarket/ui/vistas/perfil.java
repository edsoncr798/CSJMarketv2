package com.csj.csjmarket.ui.vistas;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.csj.csjmarket.LoginActivity;
import com.csj.csjmarket.MainActivity;
import com.csj.csjmarket.R;
import com.csj.csjmarket.databinding.ActivityLoginBinding;
import com.csj.csjmarket.databinding.FragmentPerfilBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;


public class perfil extends Fragment {

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private FragmentPerfilBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPerfilBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(getContext(), gso);

        mAuth = FirebaseAuth.getInstance();
        Bundle bundle = getArguments();

        String nombre = bundle.getString("nombre");
        binding.perfilTxtInicial.setText(nombre.substring(0, 1));
        binding.perfilTxtNombre.setText(nombre);
        binding.perfilTxtCorreo.setText(bundle.getString("correo"));

        binding.perfilBtnCerrarSesion.setOnClickListener(view1 -> {
            mAuth.signOut();
            googleSignInClient.revokeAccess();
            Intent intent = new Intent(getContext(), LoginActivity.class);
            startActivity(intent);
            getActivity().finish();
        });
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}