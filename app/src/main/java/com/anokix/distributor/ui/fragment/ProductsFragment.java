package com.anokix.distributor.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.data.ApiMapper;
import com.anokix.distributor.network.ApiCallback;
import com.anokix.distributor.network.ApiClient;
import com.anokix.distributor.network.dto.ProductsData;
import com.anokix.distributor.ui.NotificationsActivity;
import com.anokix.distributor.ui.adapter.ProductAdapter;

public class ProductsFragment extends Fragment {

    private ProductAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_products, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.notificationsButton).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), NotificationsActivity.class)));

        view.findViewById(R.id.addProductFab).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Add Product – coming soon", Toast.LENGTH_SHORT).show());

        RecyclerView list = view.findViewById(R.id.productsList);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ProductAdapter();
        list.setAdapter(adapter);

        loadProducts();
    }

    private void loadProducts() {
        ApiClient.get(requireContext()).getProducts(new ApiCallback<ProductsData>() {
            @Override
            public void onSuccess(ProductsData data) {
                if (isAdded()) {
                    adapter.setItems(ApiMapper.toProductItems(data));
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
