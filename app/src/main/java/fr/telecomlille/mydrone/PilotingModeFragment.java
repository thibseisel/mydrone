package fr.telecomlille.mydrone;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import fr.telecomlille.mydrone.accelerometer.AccelerometerActivity;
import fr.telecomlille.mydrone.path.PathControlActivity;

/**
 * Une boite de dialogue qui apparait en bas de l'écran lorsque l'utilisateur
 * souhaite se connecter à un drône. Elle permet de choisir parmi différents modes de pilotage
 * tels que "boutons", "joystick", "accéléromètre".
 */
public class PilotingModeFragment extends BottomSheetDialogFragment {

    private RecyclerView mRecycler;
    private PilotingModeAdapter mAdapter;
    private OnPilotingModeSelectedListener mActivity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mActivity = (OnPilotingModeSelectedListener) context;
        } catch (ClassCastException e) {
            throw new IllegalStateException(context +
                    " activity must implement OnPilotingModeSelectedListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new PilotingModeAdapter(getContext());
        mAdapter.setHasStableIds(true);
        mAdapter.setOnPilotingModeSelectedListener(mActivity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pilotingmode, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mRecycler = (RecyclerView) view.findViewById(R.id.recyclerView);
        mRecycler.setAdapter(mAdapter);
        mRecycler.setHasFixedSize(true);
    }

    @Override
    public void onDestroyView() {
        mRecycler = null;
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        mActivity = null;
        super.onDetach();
    }

    public interface OnPilotingModeSelectedListener {
        /**
         * Callback appelé lorsqu'un mode de pilotage est sélectionné.
         * Cette méthode fournit en argument l'Activity
         * qui doit être lancée pour utiliser ce mode de pilotage.
         *
         * @param targetActivity classe de l'Activity à lancer pour utiliser
         *                       le mode de pilotage sélectionné par l'utilisateur.
         */
        void onPilotingModeSelected(Class targetActivity);
    }

    /**
     * Classe permettant la mise en forme de la liste des modes de pilotage disponibles
     * dans une grille avec un RecyclerView.
     */
    private static class PilotingModeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @DrawableRes
        private static final int[] ICONS = {
                R.drawable.ic_control_button_24dp,
                R.drawable.ic_control_joystick_24dp,
                R.drawable.ic_control_accelerometer_24dp,
                R.drawable.ic_control_path_24dp
        };
        @StringRes
        private static final int[] LABELS = {
                R.string.control_buttons,
                R.string.control_joystick,
                R.string.control_accelerometer,
                R.string.control_pathdraw
        };

        private static final Class[] ACTIVITIES = new Class[]{
                ControllerActivity.class,
                JoystickActivity.class,
                AccelerometerActivity.class,
                PathControlActivity.class
        };

        private final LayoutInflater mInflater;
        private OnPilotingModeSelectedListener mListener;

        PilotingModeAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = mInflater.inflate(R.layout.grid_item_piloting, parent, false);
            return new PilotingModeHolder(v);
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            PilotingModeHolder pilotingHolder = (PilotingModeHolder) holder;
            pilotingHolder.icon.setImageResource(ICONS[position]);
            pilotingHolder.label.setText(LABELS[position]);

            pilotingHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        Class targetActivity = ACTIVITIES[holder.getAdapterPosition()];
                        mListener.onPilotingModeSelected(targetActivity);
                    }
                }
            });
        }

        void setOnPilotingModeSelectedListener(OnPilotingModeSelectedListener listener) {
            mListener = listener;
        }

        @Override
        public int getItemCount() {
            return ACTIVITIES.length;
        }
    }

    /**
     * Ensemble de vue correspondant à un item de la grille.
     * Permet l'affichage d'un mode de pilotage dans RecyclerView.
     */
    private static class PilotingModeHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView label;

        PilotingModeHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.icon);
            label = (TextView) itemView.findViewById(R.id.label);
        }
    }
}
