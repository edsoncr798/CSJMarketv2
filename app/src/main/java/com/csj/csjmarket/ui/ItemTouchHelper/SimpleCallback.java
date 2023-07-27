package com.csj.csjmarket.ui.ItemTouchHelper;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.csj.csjmarket.ui.adaptadores.itemCarritoAdapter;

public class SimpleCallback {
    public static class SwipeToDeleteCallback extends ItemTouchHelper.Callback {
        private itemCarritoAdapter adapter;

        public SwipeToDeleteCallback(itemCarritoAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            int swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
            return makeMovementFlags(0, swipeFlags);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            adapter.showDeleteButton(position);
        }

        @Override
        public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
            return 0.3f; // Ajusta este valor para cambiar la cantidad de deslizamiento requerida
        }
    }
}
