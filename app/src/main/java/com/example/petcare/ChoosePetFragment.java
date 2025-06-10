package com.example.petcare;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


public class ChoosePetFragment extends Fragment {

    View view;
    Context context;
    Button bNewPet, bConnectPet;


    public ChoosePetFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_choose_pet, container, false);
        initComponents();

        bNewPet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                GeneratePetFragment generatePetFragment = new GeneratePetFragment();
                fragmentTransaction.replace(R.id.flNewPet, generatePetFragment);
                fragmentTransaction.commit();
            }
        });

        bConnectPet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                ConnectPetFragment connectPetFragment = new ConnectPetFragment();
                fragmentTransaction.replace(R.id.flNewPet, connectPetFragment);
                fragmentTransaction.commit();
            }
        });

        return view;
    }

    private void initComponents() {
        context = getActivity();
        bConnectPet = view.findViewById(R.id.bConnectPet);
        bNewPet = view.findViewById(R.id.bNewPet);
    }
}