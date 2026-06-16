package com.anokix.distributor.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.anokix.distributor.ui.AddProductOptionsActivity;
import com.anokix.distributor.ui.CreateProductActivity;
import com.anokix.distributor.ui.NotificationsActivity;
import com.anokix.distributor.ui.adapter.ProductAdapter;

public class ProductsFragment extends Fragment {

    private ProductAdapter adapter;

    private final ActivityResultLauncher<Intent> wizardLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                    loadProducts();
                }
            });

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
                wizardLauncher.launch(new Intent(requireContext(), AddProductOptionsActivity.class)));

        RecyclerView list = view.findViewById(R.id.productsList);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ProductAdapter();
        adapter.setOnItemClickListener(item -> {
            Intent intent = new Intent(requireContext(), CreateProductActivity.class);
            intent.putExtra(CreateProductActivity.EXTRA_PRODUCT_ID, item.id);
            wizardLauncher.launch(intent);
        });
        list.setAdapter(adapter);

        EditText searchInput = view.findViewById(R.id.searchInput);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

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
