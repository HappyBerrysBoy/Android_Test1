package com.unitloadsystem.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.unitloadsystem.activitys.R;

public class Fragments {

	public static class TitleFragment extends Fragment {
		public TitleFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			return rootView;
		}
	}
	
	public static class MenuFragment extends Fragment {
		public MenuFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_sub1, container, false);
			return rootView;
		}
	}
	
	public static class UnitCalcFragment extends Fragment {
		public UnitCalcFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.unitcalculation_layout, container, false);
			return rootView;
		}
	}
	
	public static class CalculationResultFragment extends Fragment {
		public CalculationResultFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.calculationresult_layout, container, false);
			return rootView;
		}
	}
	
	public static class PalletManagerFragment extends Fragment {
		public PalletManagerFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.palletmanager_layout, container, false);
			return rootView;
		}
	}
}
