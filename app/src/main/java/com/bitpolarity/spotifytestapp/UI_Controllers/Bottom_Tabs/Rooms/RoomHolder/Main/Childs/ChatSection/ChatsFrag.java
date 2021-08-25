package com.bitpolarity.spotifytestapp.UI_Controllers.Bottom_Tabs.Rooms.RoomHolder.Main.Childs.ChatSection;

import static android.graphics.Typeface.BOLD;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.aghajari.emojiview.AXEmojiManager;
import com.aghajari.emojiview.view.AXEmojiPopup;
import com.aghajari.emojiview.view.AXSingleEmojiView;
import com.bitpolarity.spotifytestapp.Adapters.ChatsAdapter.MultiViewChatAdapter;
import com.bitpolarity.spotifytestapp.Adapters.CircleFriendActivityAdapter.UserListAdapter;
import com.bitpolarity.spotifytestapp.DB_Handler;
import com.bitpolarity.spotifytestapp.GetterSetterModels.ChatListModel_Multi;
import com.bitpolarity.spotifytestapp.LinearLayoutManagers.SigmoLinearLayoutManager;
import com.bitpolarity.spotifytestapp.R;
import com.bitpolarity.spotifytestapp.RecyclerScrollManager;
import com.bitpolarity.spotifytestapp.Singletons.TimeSystem;
import com.bitpolarity.spotifytestapp.databinding.FragmentRoomChatBinding;
import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;


public class ChatsFrag extends Fragment implements MultiViewChatAdapter.ClickListner {

    FragmentRoomChatBinding binding;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference msgRoot , isTypingRoot , bgRoot;
    MediaPlayer mpSent, mpClick ;

    RecyclerView chatRV;
    MultiViewChatAdapter adapter;
    SigmoLinearLayoutManager speedyLinearLayoutManager;

    public static DatabaseReference in_msg;
    final static String TAG = "ChatsFrag";
    private Parcelable recyclerViewState;
    TimeSystem timeSystem;
    InputMethodManager imm;

    static final int TOTAL_ELEMENT_TO_LOAD = 20;
    private final int mCurrentPage = 1;
    static boolean isTyping = false;
    ShimmerFrameLayout shimmerFrameLayout;
    ChatsViewHolder chatsViewHolder;

    long delay = 500; // 1 seconds after user stops typing
    long last_text_edit = 0;
    Handler handler = new Handler();
    StyleSpan boldSpan;
    GradientDrawable shapeHiddenReference , shapeShownReference;


    private  final String TYPE_MSG = "1";
    private  final String TYPE_JOIN = "2";
    private static final int TYPE_LEFT = R.dimen.TYPE_LEFT;
    private int lastFirstVisiblePosition;


    // Responses
    private  final int success_sent = 1;
    private  final int failed_invalid_message = 0;
    private  final int failed_internal_error = 404;

    Context context;


    private Runnable input_finish_checker = () -> {
        if (System.currentTimeMillis() > (last_text_edit + delay - 500)) {
                updateTyping();
        }
    };


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentRoomChatBinding.inflate(inflater, container , false);
        firebaseDatabase = FirebaseDatabase.getInstance();
        timeSystem = TimeSystem.getInstance();

        String roomName = getActivity().getIntent().getStringExtra("room_name");

        bgRoot = firebaseDatabase.getReference().child("roomBG");
        isTypingRoot = firebaseDatabase.getReference().child("Rooms").child(roomName).child("members");
        msgRoot= firebaseDatabase.getReference().child("Rooms").child(roomName).child("messages");

        imm= (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

       chatsViewHolder = new ViewModelProvider(this, new ChatsViewHolderFactory(getActivity().getIntent().getStringExtra("room_name"))).get(ChatsViewHolder.class);


       shimmerFrameLayout = binding.shimmerFrameLayoutChatfrag;
       shimmerFrameLayout.startShimmerAnimation();

       mpSent = MediaPlayer.create(getContext(), R.raw.hs_bg_message_sent2);
       mpClick = MediaPlayer.create(getContext(), R.raw.know);

      //getActivity().getWindow().setSoftInputMode(SOFT_INPUT_ADJUST_PAN);
       //swipeRefreshLayout = binding.swipeRefresh;
       //swipeRefreshLayout.setEnabled(false);

       chatRV = binding.chatsLayout;
       //layoutManager = new LinearLayoutManager(getContext());
       speedyLinearLayoutManager = new SigmoLinearLayoutManager(getContext());

       return binding.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);



//        layoutManager.setOrientation(RecyclerView.VERTICAL);
//        layoutManager.setStackFromEnd(true);

         boldSpan = new StyleSpan(Typeface.BOLD);
         initOnScroll_UP_JUMP_TO_TOP();

         shapeHiddenReference =  new GradientDrawable();
         shapeHiddenReference.setCornerRadius( 70 );
         shapeHiddenReference.setColor(Color.parseColor("#AD303030"));



        speedyLinearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        speedyLinearLayoutManager.setStackFromEnd(true);

        chatRV.hasFixedSize();
        chatRV.setLayoutManager(speedyLinearLayoutManager);
        chatRV.setNestedScrollingEnabled(false);
        loadmessages();

        binding.roomInput.sendBtn.setOnClickListener(v -> {
            sendMessage();
        });



        binding.roomInput.msgEditBox.addTextChangedListener(new TextWatcher() {


            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {


                handler.removeCallbacks(input_finish_checker);
                isTypingRoot.child("typingNow").setValue(DB_Handler.getUsername()) ;

            }

            @Override
            public void afterTextChanged(Editable s) {


                    last_text_edit = System.currentTimeMillis();
                    handler.postDelayed(input_finish_checker, delay);

                   // isTypingRoot.child("Divyanshu").child("isTyping").setValue("false");
            }
        });

//         swipeRefreshLayout.setOnRefreshListener(() -> {
//             mCurrentPage++;
//             chatList.clear();
//             loadmessages();
//         });

    }


    @Override
    public void onPause() {
        super.onPause();




    }

    @Override
    public void onResume() {
        super.onResume();

        ///////////////////////////////////////////////////////Emoji

        initEmojiBoard();
        //////////////////////////////////////////////////////Emoji

        handleReferenceEditBox();

        binding.jumpToEndFAB.setOnClickListener(view -> {
            onClickFab();
        });

        chatRV.addOnScrollListener(new RecyclerScrollManager.FabScroll() {

            @Override
            public void show() {
                binding.jumpToEndFAB.setVisibility(View.VISIBLE);
                binding.jumpToEndFAB.setAnimation(AnimationUtils.loadAnimation(context,R.anim.pop_in));
                binding.jumpToEndFAB.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2)).start();
            }

            @Override
            public void hide() {
                //binding.jumpToEndFAB.setVisibility(View.VISIBLE);
               // binding.jumpToEndFAB.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2)).start();
                binding.jumpToEndFAB.animate().translationY(binding.jumpToEndFAB.getHeight() +30).setInterpolator(new AccelerateInterpolator(2)).start();
            }
        });



            bgRoot.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    String url = snapshot.child("url").getValue().toString();
                    setChatWall(url);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

    }

    private void initEmojiBoard() {

        //final EmojiPopup emojiPopup = EmojiPopup.Builder.fromRootView(binding.getRoot()).build(binding.roomInput.msgEditBox);

        AXSingleEmojiView emojiView = new AXSingleEmojiView(getContext());
        emojiView.setEditText(binding.roomInput.msgEditBox);

        AXEmojiPopup emojiPopup = new AXEmojiPopup(emojiView);
        //setTheme();
        binding.roomInput.til.setStartIconOnClickListener(view -> {
            emojiPopup.toggle();

            binding.roomInput.msgEditBox.setOnClickListener(view1 -> emojiPopup.dismiss());

        });


    }

    void setTheme(){

        AXEmojiManager.getEmojiViewTheme().setFooterEnabled(true);
        AXEmojiManager.getEmojiViewTheme().setSelectionColor(Color.TRANSPARENT);
        AXEmojiManager.getEmojiViewTheme().setSelectedColor(0xff82ADD9);
        AXEmojiManager.getEmojiViewTheme().setFooterSelectedItemColor(0xff82ADD9);
        AXEmojiManager.getEmojiViewTheme().setBackgroundColor(0xFF1E2632);
        AXEmojiManager.getEmojiViewTheme().setCategoryColor(0xFF232D3A);
        AXEmojiManager.getEmojiViewTheme().setFooterBackgroundColor(0xFF232D3A);
        AXEmojiManager.getEmojiViewTheme().setVariantPopupBackgroundColor(0xFF232D3A);
        AXEmojiManager.getEmojiViewTheme().setVariantDividerEnabled(false);
        AXEmojiManager.getEmojiViewTheme().setDividerColor(0xFF1B242D);
        AXEmojiManager.getEmojiViewTheme().setDefaultColor(0xFF677382);
        AXEmojiManager.getEmojiViewTheme().setTitleColor(0xFF677382);
        AXEmojiManager.getEmojiViewTheme().setAlwaysShowDivider(true);

    }

    void onClickFab(){
        int pos = chatsViewHolder.getListSize();
        Log.d(TAG, "onClickFab: POSITION "+pos);
        speedyLinearLayoutManager.smoothScrollToPosition(chatRV, (RecyclerView.State) recyclerViewState,pos-1);
        //chatRV.smoothScrollToPosition(listSize-1);
        binding.jumpToEndFAB.animate().translationY(binding.jumpToEndFAB.getHeight() +30).setInterpolator(new AccelerateInterpolator(5)).start();
        RecyclerScrollManager.FabScroll.setScrollDist();

    }



    void handleReferenceEditBox(){

        binding.roomInput.referenceLayout.cancelReference.setOnClickListener(view -> {
                reference_onCancel();
        });
    }

    void reference_onCancel(){
        //binding.roomInput.referenceLayout.referenceView.animate().translationY(binding.roomInput.referenceLayout.referenceView.getHeight()+30).setInterpolator(new AccelerateInterpolator(2)).start();
        binding.roomInput.msgEditBox.setBackground(shapeHiddenReference);
        //
        // binding.roomInput.referenceLayout.referenceView.setAnimation(AnimationUtils.loadAnimation(context,R.anim.slide_down));
        binding.roomInput.referenceLayout.referenceView.setVisibility(View.GONE);

        //new Handler().postDelayed(() -> binding.roomInput.referenceLayout.referenceView.setVisibility(View.GONE),100);
        //  binding.jumpToEndFAB.animate().translationY(binding.jumpToEndFAB.getHeight() +30).setInterpolator(new AccelerateInterpolator(2)).start();

    }

    void showReference(ArrayList<ChatListModel_Multi> chatlist , int pos){
       // binding.roomInput.referenceLayout.referenceView.animate().translationY(-binding.roomInput.referenceLayout.referenceView.getHeight()).setInterpolator(new AccelerateInterpolator(2)).start();

        String reference_user = chatlist.get(pos).getSenderName();
        String msg = chatlist.get(pos).getMessage();

        binding.roomInput.referenceLayout.referenceView.setVisibility(View.VISIBLE);
        binding.roomInput.msgEditBox.setBackgroundResource(R.drawable.editbox_bg_reference);
        binding.roomInput.referenceLayout.referenceText.setText(msg);
        binding.roomInput.referenceLayout.referenceToUsername.setText("@ "+reference_user);
        binding.roomInput.referenceLayout.referenceView.setAnimation(AnimationUtils.loadAnimation(context,R.anim.slide_up));
        binding.roomInput.referenceLayout.referenceText.setAnimation(AnimationUtils.loadAnimation(context,R.anim.slide_up_slo));
        binding.roomInput.referenceLayout.referenceToUsername.setAnimation(AnimationUtils.loadAnimation(context,R.anim.slide_up_slower));

        binding.roomInput.msgEditBox.requestFocus();
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);

        binding.roomInput.referenceLayout.referenceText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speedyLinearLayoutManager.smoothScrollToPosition(chatRV, (RecyclerView.State) recyclerViewState,pos);
            }
        });



    }

    void handleEmojiView(){



//        if(!layout.isShowing()) {




//            layout.show();
//            layout.setVisibility(View.VISIBLE);
//
//            binding.roomInput.msgEditBox.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    layout.hideAndOpenKeyboard();
//                    layout.toggle();
//                    layout.setVisibility(View.GONE);
//
//
//                }
//            });
//
//        }
//
//        else {
//            //layout.setVisibility(View.GONE);
//            layout.hideAndOpenKeyboard();
//            //layout.dismiss();
//        }

    }

    void setChatWall(String url){

        Glide.with(context)
                .load(url)
                .into(binding.roomBackgroundWallpaper);
    }


    void loadmessages(){

                chatsViewHolder.postMessages();

                   chatsViewHolder.getChatListLD().observe(getViewLifecycleOwner(), chatListModel_multis -> {

                       adapter = new MultiViewChatAdapter(chatListModel_multis, ChatsFrag.this);

                       chatRV.setVisibility(View.VISIBLE);
                       chatRV.setAdapter(adapter);
                       adapter.notifyDataSetChanged();

                       shimmerFrameLayout.stopShimmerAnimation();
                       shimmerFrameLayout.setVisibility(View.GONE);

                       if(binding.jumpToEndFAB.getVisibility()==View.VISIBLE) {
                            binding.jumpToEndFAB.setAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_out));
                           binding.jumpToEndFAB.setVisibility(View.GONE);
                       }

                   });




    }

    void sendMessage(){

        binding.roomInput.sendBtn.setAnimation(AnimationUtils.loadAnimation(context,R.anim.pop_in));
        String msg = binding.roomInput.msgEditBox.getText().toString();
        String usrname = DB_Handler.getUsername();
        String time = timeSystem.getTime_format_12h();


       int response =  chatsViewHolder.sendMessage(msg,usrname,time);

       switch (response){

           case success_sent:
                 binding.roomInput.msgEditBox.setText("");
                 mpSent.start();
                 break;

           case failed_internal_error:
               binding.roomInput.msgEditBox.setError("Internal error , restart sigmo!");
               break;

           case failed_invalid_message:

               binding.roomInput.msgEditBox.setError("Message invalid!");
               binding.roomInput.sendBtn.setAnimation(AnimationUtils.loadAnimation(context,R.anim.pop_out));
               break;

           default:
               binding.roomInput.msgEditBox.setError("Unknow error, retry!");


       }

    }

    void updateTyping(){

        isTypingRoot.child("typingNow").setValue("");
    }

    void getTypingMembers(){

        isTypingRoot.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                   Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                //Log.d(TAG, "onDataChange: Map ChatsFrag"+map.toString());

                for (String key : map.keySet()){
                    Log.d(TAG, "onDataChange: KEYS"+ key+" : "+ map.get(key));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void initOnScroll_UP_JUMP_TO_TOP() {

        chatRV.addOnScrollListener(new RecyclerScrollManager.MiniplayerScroll() {

            //Move Miniplayer up and show jump to top tab

            @Override
            public void show() {
                binding.miniPlayerRoom.jumpToTop.setVisibility(View.VISIBLE);
                binding.miniPlayerRoom.jumpToTop.setAnimation(AnimationUtils.loadAnimation(context,R.anim.pop_in_jump_to_top));
                binding.miniPlayerRoom.getRoot().animate().translationY(-binding.miniPlayerRoom.getRoot().getHeight()).setInterpolator(new AccelerateInterpolator(2)).start();


            }

            //show Miniplayer up and hide jump to top tab

            @Override
            public void hide() {
                binding.miniPlayerRoom.jumpToTop.setAnimation(AnimationUtils.loadAnimation(context,R.anim.fade_out));
                binding.miniPlayerRoom.jumpToTop.setVisibility(View.GONE);
                binding.miniPlayerRoom.getRoot().animate().translationY(0).setInterpolator(new DecelerateInterpolator(2)).start();

            }
        });


        binding.miniPlayerRoom.jumpToTop.setOnClickListener(view -> {
            chatRV.smoothScrollToPosition(0);
            binding.miniPlayerRoom.getRoot().animate().translationY(0).setInterpolator(new DecelerateInterpolator(2)).start();
            binding.miniPlayerRoom.jumpToTop.setVisibility(View.GONE);
            RecyclerScrollManager.MiniplayerScroll.setScrollDist();
        });

    }


    @Override
    public void onDetach() {
        super.onDetach();

    }


    @Override
    public void onClick(int position) {
        //Toast.makeText(context, "Touched  :"+position, Toast.LENGTH_SHORT).show();

        showReference(chatsViewHolder.getChatList(), position);
    }
}




// Query msgQue = msgRoot.limitToLast(mCurrentPage*TOTAL_ELEMENT_TO_LOAD);


//recyclerViewState = chatRV.getLayoutManager().onSaveInstanceState();
//int size = getModelList(snapshot).size();


//chatRV.getLayoutManager().onRestoreInstanceState(recyclerViewState);

//swipeRefreshLayout.setRefreshing(false);
//chatRV.scrollToPosition(listSize- 1);
