package clain.boris.myGames.skTBM;

//Les imports concernant Java
import java.util.ArrayList;
import java.util.HashSet;
//Les imports concernant Android
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
//Les imports concernant l'API
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.ParticipantResult;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchUpdateReceivedListener;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchBuffer;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.games.multiplayer.turnbased.LoadMatchesResponse;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer.LoadMatchesResult;
//Les imports concernant la librairie BaseGameUtils
import com.google.example.games.basegameutils.BaseGameUtils;


/*
    Activité principale
    Se charge de l'affichage de l'interface graphique, du lien entre l'interface graphique et
    de la classe GameBoard, ainsi que de la communication avec le serveur de Google
    par l'API Play Game Services
 */

public class MainActivty extends Activity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        OnInvitationReceivedListener, OnTurnBasedMatchUpdateReceivedListener,
        View.OnClickListener {

    public static final String TAG = "Slow Chess";

    // Objet représentant le clien pour interagir avec l'API
    private GoogleApiClient mGoogleApiClient;

    private boolean mResolvingConnectionFailure = false;

    private boolean mSignInClicked = false;
    // Débuter le sign-in automatiquement
    private boolean mAutoStartSignInFlow = true;


    private TurnBasedMatch mTurnBasedMatch;

    //Déclaration des widgets de l'activité
    private ImageButton validateMoveButton;
    private ImageButton cancelMoveButton;
    private Button askForRematchButton;
    private Button checkGamesButton;
    private ImageButton forfeitButton;
    private TextView showTurn;
    private TextView showTurnColor;
    private AlertDialog mAlertDialog;
    private TextView checkWarning;
    private TextView matWarning;
    private TextView [] horizontalNotation ;
    private TextView [] verticalNotation ;

    // Des constantes pour l'API de Google
    private static final int RC_SIGN_IN = 9001;
    final static int RC_SELECT_PLAYERS = 10000;
    final static int RC_LOOK_AT_MATCHES = 10001;

    //Durée des toasts.
    final static int TOAST_DELAY = Toast.LENGTH_SHORT;

    public boolean showTheGameplay = false;
    private boolean hasAMoveToUndo = false;
    private String proposedMove = "";
    private boolean gameOver = false;
    private boolean myTurn = false;
    private boolean whiteTurn = true;
    private boolean mustEnd = false;

    //Match actuel
    public TurnBasedMatch mMatch;

    //Données du match actuel
    public GameBoard mTurnData;

    //Indique si une pièce est sélectionnée sur le board
    private boolean aPieceIsAlreadySelected = false;
    private int rowOfPieceSelected = -1;
    private int columnOfPieceSelected = -1;
    private int idLastSelectedRessource = -1;
    private char chosenPromotion = ' ';


    /*
    Méthode usuelle appelée lors de la création de l'activité.
    Se charge de relier la ressource xml du layout et ses composantes aux variables
    d'instance correspondantes. Se charge également de définir certain listeners sur certains
    boutons.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_chess);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.sign_in_button).setOnClickListener(this);

        validateMoveButton = ((ImageButton) findViewById(R.id.validateMoveButton));
        cancelMoveButton = ((ImageButton) findViewById(R.id.cancelMoveButton));
        forfeitButton = ((ImageButton) findViewById(R.id.forfeitButton));
        showTurn = (TextView) findViewById(R.id.showTurn);
        showTurnColor = (TextView) findViewById(R.id.showTurnColor);
        askForRematchButton = ((Button) findViewById(R.id.askForRematchButton));
        checkWarning = ((TextView) findViewById(R.id.checkWarning));
        matWarning = ((TextView) findViewById(R.id.matWarning));
        checkGamesButton = ((Button) findViewById(R.id.checkGamesButton));

        GameBoard.gameImageViews = new ImageView[8][8];
        int ids[] = {
                R.id.a8, R.id.b8, R.id.c8, R.id.d8, R.id.e8, R.id.f8, R.id.g8, R.id.h8,
                R.id.a7, R.id.b7, R.id.c7, R.id.d7, R.id.e7, R.id.f7, R.id.g7, R.id.h7,
                R.id.a6, R.id.b6, R.id.c6, R.id.d6, R.id.e6, R.id.f6, R.id.g6, R.id.h6,
                R.id.a5, R.id.b5, R.id.c5, R.id.d5, R.id.e5, R.id.f5, R.id.g5, R.id.h5,
                R.id.a4, R.id.b4, R.id.c4, R.id.d4, R.id.e4, R.id.f4, R.id.g4, R.id.h4,
                R.id.a3, R.id.b3, R.id.c3, R.id.d3, R.id.e3, R.id.f3, R.id.g3, R.id.h3,
                R.id.a2, R.id.b2, R.id.c2, R.id.d2, R.id.e2, R.id.f2, R.id.g2, R.id.h2,
                R.id.a1, R.id.b1, R.id.c1, R.id.d1, R.id.e1, R.id.f1, R.id.g1, R.id.h1,
        };
        for(int i=0; i<64; i++){
            GameBoard.gameImageViews[i/8][i%8] = (ImageView) findViewById(ids[i]);
        }
        for(int i = 0; i<8; i++){
            for(int j =0 ; j<8; j++){
                GameBoard.gameImageViews[i][j].setOnClickListener(this);
            }
        }

        horizontalNotation = new TextView[8];
        verticalNotation = new TextView[8];
        int horizontalIds[] = {
                R.id.H1, R.id.H2, R.id.H3, R.id.H4, R.id.H5, R.id.H6, R.id.H7, R.id.H8
        };
        int verticalIds[] = {
                R.id.V1,  R.id.V2,  R.id.V3,  R.id.V4,  R.id.V5,  R.id.V6,  R.id.V7,  R.id.V8
        };
        for(int i =0 ; i < 8 ; i++){
                horizontalNotation[i] = (TextView) findViewById(horizontalIds[i]);
                verticalNotation[i] = (TextView) findViewById(verticalIds[i]);
        }



        disableMyTwoButtons();
        askForRematchButton.setVisibility(View.INVISIBLE);
        askForRematchButton.setClickable(false);
    }


    /*
    À chaque démarrage de l'activité, se connecte à l'API de Google
     */
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart(): Connecting to Google APIs");
        mGoogleApiClient.connect();
    }


    /*
    À la fermeture de l'app., remise de certaines variables d'état de l'activité à leur valeur par défaut
     */
    @Override
    protected void onStop() {
        super.onStop();
        redrawSelectedPieceAsStandard(whiteTurn);
        aPieceIsAlreadySelected = false;
        rowOfPieceSelected = -1;
        columnOfPieceSelected = -1;
        idLastSelectedRessource = -1;
        try {
            mTurnData.resetColorOfSquares();
        } catch ( Exception e ){
        }

        Log.d(TAG, "onStop(): Disconnecting from Google APIs");
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /*
        Lorsque la connexion à l'API est réussie, met en place des listeners sur les invitations
        à faire des parties ansi qu'aux mises-à-jour des parties
    */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected(): Connexion réussie");

        // Retrieve the TurnBasedMatch from the connectionHint
        if (connectionHint != null) {
            mTurnBasedMatch = connectionHint.getParcelable(Multiplayer.EXTRA_TURN_BASED_MATCH);

            if (mTurnBasedMatch != null) {
                if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
                    Log.d(TAG, "Attention : tentative de connexion impossible");
                }
                updateMatch(mTurnBasedMatch);
                return;
            }
        }
        setViewVisibility();
        Games.Invitations.registerInvitationListener(mGoogleApiClient, this);
        Games.TurnBasedMultiplayer.registerMatchUpdateListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended():  Tentative de reconnexion.");
        mGoogleApiClient.connect();
        setViewVisibility();
    }

    /*
        Si la connexion a échoué, s'occupe de réessayer
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed(): Tentative de reprise");
        if (mResolvingConnectionFailure) {
            // Already resolving
            Log.d(TAG, "onConnectionFailed(): En train de résoudre.");
            return;
        }

        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;

            mResolvingConnectionFailure = BaseGameUtils.resolveConnectionFailure(this,
                    mGoogleApiClient, connectionResult, RC_SIGN_IN,
                    getString(R.string.signin_other_error));
        }

        setViewVisibility();
    }



    // // // // // // // // // // // // // // // //
    //Méthodes de contrôle dans le menu principal //
    // // // // // // // // // // // // // // // //


    /* Lors d'un clic sur le bouton "Jouer avec un ami", ouvre l'activité de sélection d'un ami
    pré-configurée par Google et permet au joueur la sélection d'un maximum d'un ami. Après
    sélection d'un choix, envoie le résultat à la méthode processResult() correspondante.
*/
    public void onStartMatchClicked(View view) {
        Intent intent = Games.TurnBasedMultiplayer.getSelectOpponentsIntent(mGoogleApiClient,
                1, 1, true);
        startActivityForResult(intent, RC_SELECT_PLAYERS);
    }


    /* Lors d'un clic sur le bouton "Mes Parties", ouvre l'activité du "inbox", c'est-à-dire
        de la boîte aux lettres pré-configurée par Google (recommandé par Google) qui
        permet d'examiner les parties en cours et les parties terminées, et attend
        le clic de l'usager avant d'envoyer le résultat à la méthode processResult()
         correspondante.
    */
    public void onCheckGamesClicked(View view) {
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
        try {
            Intent intent = Games.TurnBasedMultiplayer.getInboxIntent(mGoogleApiClient);
            startActivityForResult(intent, RC_LOOK_AT_MATCHES);
            dismissSpinner();
            setViewVisibility();
        }catch(Exception e){
            setViewVisibility();
        }
    }


    /*
        Lors d'un clic sur le bouton "Trouver un adversaire", démarre automatiquement un match
        contre un adversaire
     */
    public void onQuickMatchClicked(View view) {

        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                1, 1, 0);

        TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
                .setAutoMatchCriteria(autoMatchCriteria).build();

        showSpinner();

        // Start the match
        ResultCallback<TurnBasedMultiplayer.InitiateMatchResult> cb = new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {
            @Override
            public void onResult(TurnBasedMultiplayer.InitiateMatchResult result) {
                processResult(result);
            }
        };
        Games.TurnBasedMultiplayer.createMatch(mGoogleApiClient, tbmc).setResultCallback(cb);
    }


    // // // // // // // // // // // // // //
    //Méthodes de contrôle dans une partie //
    // // // // // // // // // // // // // //


    /*
    Lors du clic sur le bouton Rematch, débute une autre partie et envoie
    l'invittion à l'adversaire
    */
    public void onAskForRematchClicked(View view){
        aPieceIsAlreadySelected = false;
        idLastSelectedRessource = -1;
        columnOfPieceSelected = -1;
        rowOfPieceSelected = -1;
        rematch();
    }



    /*
      Lors du clic sur le bouton de retour au menu, retourne au menu
      (pas d'appel à l'API)
     */
    public void onBackToMenuClicked(View view){
        redrawSelectedPieceAsStandard(whiteTurn);
        aPieceIsAlreadySelected = false;
        idLastSelectedRessource = -1;
        columnOfPieceSelected = -1;
        rowOfPieceSelected = -1;
        try {
            mTurnData.resetColorOfSquares();
        } catch ( Exception e ){
        }
        if(mustEnd){
            Games.TurnBasedMultiplayer.finishMatch( mGoogleApiClient, mMatch.getMatchId());
            mustEnd = false;
        }
        showTheGameplay = false;
        setViewVisibility();
    }

    /*
        Lors du clic sur le bouton de retour au "inbox", c-a-d la boît aux lettres,
        retourne au inbox.
        (appel à l'API)
     */
    public void onBackToMatchesClicked(View view){
        redrawSelectedPieceAsStandard(whiteTurn);
        aPieceIsAlreadySelected = false;
        idLastSelectedRessource = -1;
        columnOfPieceSelected = -1;
        rowOfPieceSelected = -1;
        try {
            mTurnData.resetColorOfSquares();
        } catch ( Exception e ){
        }
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
        if(mustEnd){
            Games.TurnBasedMultiplayer.finishMatch( mGoogleApiClient, mMatch.getMatchId());
            mustEnd = false;
        }
        showTheGameplay = true;
        try {
            Intent intent = Games.TurnBasedMultiplayer.getInboxIntent(mGoogleApiClient);
            startActivityForResult(intent, RC_LOOK_AT_MATCHES);
            dismissSpinner();
            setViewVisibility();
        }catch(Exception e){
            setViewVisibility();
        }
    }

    /*
        Lors du clic sur le bouton de validation du coup, si le coup n'est pas un coup qui mène à
        la fin de la partie, tramsforme l'objet GameBoard en sa forme transférable et joue son
        tour en ligne en l'envoyant à l'aide de la méthode takeTurn() de l'API de Google

        Si le coup était un coup qui mettait fin à la partie, l'objet GameBoard est transformé en
        sa forme transférable, mais la fin de partie est appelée en ligne en l'envoyant à l'aide de
        la méthode FinishMatch (voir la méthode endGame() ) avec le bon code (victoire/défaite/nulle)
     */
    public void onValidateMoveClicked(View view) {
        if(hasAMoveToUndo && !proposedMove.equals("")) {
            checkWarning.setVisibility(View.INVISIBLE);
            showSpinner();
            String nextParticipantId = getNextParticipantId();
            Log.d("LOCKEDMOVE", "C'est un lockedMove");
            mTurnData.lockMove(proposedMove);
            mTurnData.turnCounter += 1;
            mTurnData.isWhiteTurn = !mTurnData.isWhiteTurn;

            //si c'est au tour des Blancs et qu'ils ne peuvent jouer aucun coup
            if(mTurnData.isWhiteTurn){
                if(!playerCanMakeAMove(GameBoard.PlayerColor.WHITE)) {
                    if(mTurnData.whiteKingIsInCheck()){
                        showEndMessage("Échec et mat!", " Vous avez gagné!");
                        showTurn.setText("Vous avez gagné!");
                        showTurnColor.setText("");
                        mTurnData.winner = 1; //Les Noirs sont gagnants
                        mTurnData.gameOver = true;
                        mTurnData.encodeFromBoardToData();
                        endGame(true, false, false);
                    }else{
                        showEndMessage("Pat!", " Partie nulle");
                        showTurn.setText("Partie nulle.");
                        showTurnColor.setText("");
                        mTurnData.winner = -1; //C'est une partie nulle
                        mTurnData.gameOver = true;
                        mTurnData.encodeFromBoardToData();
                        endGame(false, false, true);
                    }
                    hasAMoveToUndo = false;
                    disableMyTwoButtons();
                    disableForfeitButton();
                    myTurn = false;
                    mTurnData = null;
                    return;
                }
            }
            //si c'est au tour des Noirs et qu'ils ne peuvent jouer aucun coup
            else {
                if (!playerCanMakeAMove(GameBoard.PlayerColor.BLACK)) {
                    if(mTurnData.blackKingIsInCheck()){
                        showEndMessage("Échec et mat!", " Vous avez gagné!");
                        showTurn.setText("Vous avez gagné!");
                        showTurnColor.setText("");
                        mTurnData.winner = 0; //Les Blancs sont gagnants
                        mTurnData.gameOver = true;
                        mTurnData.encodeFromBoardToData();
                        endGame(true, false, false);
                    } else{
                        showEndMessage("Pat!", " Partie nulle");
                        showTurn.setText("Partie nulle");
                        showTurnColor.setText("");
                        mTurnData.winner = -1; //C'est une partie nulle
                        mTurnData.gameOver = true;
                        mTurnData.encodeFromBoardToData();
                        endGame(false, false, true);
                    }
                    hasAMoveToUndo = false;
                    disableMyTwoButtons();
                    disableForfeitButton();
                    myTurn = false;
                    mTurnData = null;
                    return;
                }
            }
            mTurnData.encodeFromBoardToData();
            disableMyTwoButtons();
            hasAMoveToUndo = false;
            //takeTurn
            Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, mMatch.getMatchId(),
                    mTurnData.persist(), nextParticipantId).setResultCallback(
                    new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                        @Override
                        public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                            processResult(result);
                        }
                    });
            mTurnData = null;
        }
    }


    /*
        Annonce la fin de la partie et le résultat pour le joueur actuel (victoire, défaite, nulle)
        (API)
     */
    private void endGame(boolean win, boolean lost, boolean staleMate) {

        String playerId = Games.Players.getCurrentPlayerId(mGoogleApiClient);
        String myParticipantId = mMatch.getParticipantId(playerId);
        String nextParticipantId = getNextParticipantId();

        if(win) {
            Games.TurnBasedMultiplayer.finishMatch(mGoogleApiClient, mMatch.getMatchId(), mTurnData.persist(),
                    new ParticipantResult[]{
                            new ParticipantResult(myParticipantId, ParticipantResult.MATCH_RESULT_WIN, ParticipantResult.PLACING_UNINITIALIZED),
                            new ParticipantResult(nextParticipantId, ParticipantResult.MATCH_RESULT_LOSS, ParticipantResult.PLACING_UNINITIALIZED)
                    }).setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                @Override
                public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                    processResult(result);
                }
            });
        }
        else if(lost){
            Games.TurnBasedMultiplayer.finishMatch(mGoogleApiClient, mMatch.getMatchId(), mTurnData.persist(),
                    new ParticipantResult[]{
                            new ParticipantResult(myParticipantId, ParticipantResult.MATCH_RESULT_LOSS, ParticipantResult.PLACING_UNINITIALIZED),
                            new ParticipantResult(nextParticipantId, ParticipantResult.MATCH_RESULT_WIN, ParticipantResult.PLACING_UNINITIALIZED)
                    }).setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                @Override
                public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                    processResult(result);
                }
            });
        }
        else if (staleMate){
            Games.TurnBasedMultiplayer.finishMatch(mGoogleApiClient, mMatch.getMatchId(), mTurnData.persist(),
                    new ParticipantResult[]{
                            new ParticipantResult(myParticipantId, ParticipantResult.MATCH_RESULT_TIE, ParticipantResult.PLACING_UNINITIALIZED),
                            new ParticipantResult(nextParticipantId, ParticipantResult.MATCH_RESULT_TIE, ParticipantResult.PLACING_UNINITIALIZED)
                    }).setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                @Override
                public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                    processResult(result);
                }
            });
        }

    }


    /*
        Lors du clic sur le bouton de reprise d'un coup, reprend le coup si ce coup
        n'a pas encore été envoyé au serveur de Google par un clic sur le bouton de validation
    */
    public void onCancelMoveClicked(View view) {
        if(hasAMoveToUndo && !proposedMove.equals("")){
            mTurnData.undoAMove(proposedMove);
            if(whiteTurn) {
                mTurnData.displayForWhite();
            }
            else{
                mTurnData.displayForBlack();
            }

            disableMyTwoButtons();
            proposedMove = "";
            hasAMoveToUndo = false;
        }
    }



    /*
        Lors d'un clic sur le bouton d'abandon, abandonne si confirmation par l'usager (défaite pour soi
        et victoire pour l'adversaire)
        (appel à l'API)
     */
    public void onForfeitClicked(View view) {
        showForfeitWarning();
    }



    /*
        S'occupe de mettre en place l'interface graphique de l'écran de la partie
     */

    public void setGameplayUI(boolean forWhite) {
        dismissSpinner();
        showTheGameplay = true;
        setViewVisibility();
        checkWarning.setVisibility(View.INVISIBLE);
        matWarning.setVisibility(View.INVISIBLE);
        askForRematchButton.setClickable(false);
        askForRematchButton.setVisibility(View.INVISIBLE);

        if(forWhite){
            setNotation(true);
            mTurnData.displayForWhite();
        }
        else {
            setNotation(false);
            mTurnData.displayForBlack();
        }

        if(myTurn) {
            disableMyTwoButtons();
            enableForfeitButton();
            showTurn.setTextColor(Color.parseColor("#34a853"));
            showTurnColor.setTextColor(Color.parseColor("#34a853"));
            if(thereIsACheck(whiteTurn)){
                checkWarning.setVisibility(View.VISIBLE);
            };
            showTurn.setText("C'est à vous");
            if (mTurnData.isWhiteTurn)
                showTurnColor.setText("(les Blancs)");
            else
                showTurnColor.setText("(les Noirs)");
        }
        else {
            disableMyTwoButtons();
            disableForfeitButton();
            showTurn.setTextColor(Color.parseColor("#ffffff"));
            showTurnColor.setTextColor(Color.parseColor("#ffffff"));
            checkWarning.setVisibility(View.INVISIBLE);
            showTurn.setText("C'est à " + mMatch.getParticipant(mMatch.getPendingParticipantId()).getDisplayName());
            if (mTurnData.isWhiteTurn)
                showTurnColor.setText("(les Blancs)");
            else
                showTurnColor.setText("(les Noirs)");
        }

    }


    /*
        Les quatre méthose suivantes sont des méthodes utilitaires pour gérer l'état des
        boutons selon que le joueur peut jouer ou non et selon qu'il peut envoyer un coup
        ou non
     */

    private void disableForfeitButton() {
        forfeitButton.setClickable(false);
        forfeitButton.setImageResource(R.drawable.forfeit_disabled);
    }

    private void enableForfeitButton(){
        forfeitButton.setClickable(true);
        forfeitButton.setImageResource(R.drawable.forfeit);
    }

    private void disableMyTwoButtons() {
        validateMoveButton.setClickable(false);
        validateMoveButton.setImageResource(R.drawable.my_check_mark_disabled);
        cancelMoveButton.setClickable(false);
        cancelMoveButton.setImageResource(R.drawable.takeback_move_disabled);
    }

    private void enableMyTwoButtons() {
        validateMoveButton.setClickable(true);
        validateMoveButton.setImageResource(R.drawable.my_check_mark_green);
        cancelMoveButton.setClickable(true);
        cancelMoveButton.setImageResource(R.drawable.takeback_move);
    }

    /*
    Méthode appelée lors du changement d'écran
    Affiche l'écran si la variable d'instance showTheGameplay est à false, et l'écran du jeu si elle
    est à true.
     */
    public void setViewVisibility() {
        boolean isSignedIn = (mGoogleApiClient != null) && (mGoogleApiClient.isConnected());

        if (!isSignedIn) {
            findViewById(R.id.login_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.matchup_layout).setVisibility(View.GONE);
            findViewById(R.id.gameplay_layout).setVisibility(View.GONE);

            if (mAlertDialog != null) {
                mAlertDialog.dismiss();
            }
            return;
        }


        ((TextView) findViewById(R.id.name_field)).setText(Games.Players.getCurrentPlayer(
                mGoogleApiClient).getDisplayName());
        findViewById(R.id.login_layout).setVisibility(View.GONE);

        if (showTheGameplay) {
            findViewById(R.id.matchup_layout).setVisibility(View.GONE);
            findViewById(R.id.gameplay_layout).setVisibility(View.VISIBLE);
        } else {
            updateMyTurnMatchesNumber();
            findViewById(R.id.matchup_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.gameplay_layout).setVisibility(View.GONE);
        }
    }

    /*
     Méthode appelée par setViewVisibility lorsqu'il s'agit de changer d'écran et d'afficher l'écran
     du menu principal.
     La méthode updateMyTurnMatchesNumber() appelle l'API Google et lui demande de charger toutes les
     parties du client et de les envoyer à la méthode processResult() correspondante.
     La méthode processResult() correspondante compte ensuite toutes les parties où c'est au tour du
     joueur de jouer et modifie l'interface graphique du menu principal en conséquence.
     */
    private void updateMyTurnMatchesNumber(){
        Games.TurnBasedMultiplayer.loadMatchesByStatus(mGoogleApiClient, new int[]{TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN})
                .setResultCallback(new ResultCallback<LoadMatchesResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.LoadMatchesResult result) {
                        processResult(result);
                    }
                });
    }


    /*
        Les deux méthodes suivantes servent à montrer ou cacher les spinners
     */
    public void showSpinner() {
        findViewById(R.id.progressLayout).setVisibility(View.VISIBLE);
    }

    public void dismissSpinner() {
        findViewById(R.id.progressLayout).setVisibility(View.GONE);
    }


    /*
        Méthode d'affichage d'une boîte de dialogue d'alerte dans le but de transférer
        un message de warning
    */
    public void showWarning(String title, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle(title).setMessage(message);

        // set dialog message
        alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, close
                        // current activity
                        dialog.dismiss();
                    }
                });

        // create alert dialog
        mAlertDialog = alertDialogBuilder.create();

        // show it
        mAlertDialog.show();
    }


    /*
        Méthode d'affichage d'une boîte de dialogue d'alerte dans le but de transférer
        un message de validation de l'abandon
    */
    public void showForfeitWarning() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setTitle("Attention!").setMessage("Voulez-vous vraiment abandonner?");

        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("Oui",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                if(mTurnData != null) {
                                    mTurnData.forfeit = true;
                                    if(mTurnData.isWhiteTurn)
                                        mTurnData.winner = 0;
                                    else
                                        mTurnData.winner = 1;
                                    mTurnData.isWhiteTurn = !mTurnData.isWhiteTurn;
                                    mTurnData.persist();
                                    endGame(false, true, false);
                                    showTheGameplay = false;
                                    setViewVisibility();
                                }
                            }
                        })
                .setNegativeButton("Non",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });

        alertDialogBuilder.show();
    }

    /*
        Méthode d'affichage d'une boîte de dialogue d'alerte dans le but de transférer
        un message de validation de l'abandon
    */
    public void showPromotionSelection() {
        final CharSequence[] items = {"Reine", "Fou", "Tour", "Cavalier"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Colors");
        builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Reine")) {
                    if (whiteTurn) {
                        chosenPromotion = 'Q';
                    } else {
                        chosenPromotion = 'q';
                    }
                } else if (items[item].equals("Fou")) {
                    if (whiteTurn) {
                        chosenPromotion = 'B';
                    } else {
                        chosenPromotion = 'b';
                    }
                    dialog.dismiss();
                } else if (items[item].equals("Tour")) {
                    if (whiteTurn) {
                        chosenPromotion = 'R';
                    }
                    else{
                        chosenPromotion = 'r';
                    }
                    dialog.dismiss();
                } else if (items[item].equals("Cavalier")) {
                    if (whiteTurn) {
                        chosenPromotion = 'K';
                    }
                    else{
                        chosenPromotion = 'k';

                    }
                }
                proposedMove = proposedMove.substring(0,6) + chosenPromotion;
                mTurnData.makeAMove(proposedMove);
                mTurnData.displayForWhite();
                aPieceIsAlreadySelected = false;
                hasAMoveToUndo = true;
                enableMyTwoButtons();
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }


    /*
        Méthode appelée (par "callback) lorsque l'usager a fait un choix dans l'activité du "inbox",
        c-a-d la boîte aux lettres des parties, ou lorsqu'il a fait un choix dans l'activité de sélection
        du joueur adverse parmi ses amis.
        Donc cette méthode fait l'articulation entre les clics de l'usager dans l'interface graphique pré-
        configurée de Google et l'appel des méthodes updateMatch() et startMatch(), selon le clic.
        @request : le code qui avait été demandé
        @response : le code qui a été retourné
        @data : l'intent qui a été retourné en réponse
    */
    @Override
    public void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);
        if (request == RC_SIGN_IN) {
            mSignInClicked = false;
            mResolvingConnectionFailure = false;
            if (response == Activity.RESULT_OK) {
                mGoogleApiClient.connect();
            } else {
                BaseGameUtils.showActivityResultError(this, request, response, R.string.signin_other_error);
            }
        } else if (request == RC_LOOK_AT_MATCHES) {

            if (response != Activity.RESULT_OK) {
                Log.d("Activity", "l'usager a quitté cette activité de sélection");
                //L'usager a cliqué sur la flêche de retour
                return;
            }

            TurnBasedMatch match = data.getParcelableExtra(Multiplayer.EXTRA_TURN_BASED_MATCH);

            if (match != null) {
                updateMatch(match);
            }

            Log.d(TAG, "Match = " + match);
        } else if (request == RC_SELECT_PLAYERS) {
            // Returned from 'Select players to Invite' dialog

            if (response != Activity.RESULT_OK) {
                // user canceled
                return;
            }

            // get the invitee list
            final ArrayList<String> invitees = data
                    .getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);

            // get automatch criteria
            Bundle autoMatchCriteria = null;

            int minAutoMatchPlayers = data.getIntExtra(
                    Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
            int maxAutoMatchPlayers = data.getIntExtra(
                    Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

            if (minAutoMatchPlayers > 0) {
                autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                        minAutoMatchPlayers, maxAutoMatchPlayers, 0);
            } else {
                autoMatchCriteria = null;
            }

            TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
                    .addInvitedPlayers(invitees)
                    .setAutoMatchCriteria(autoMatchCriteria).build();

            // Start the match
            Games.TurnBasedMultiplayer.createMatch(mGoogleApiClient, tbmc).setResultCallback(
                    new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {
                        @Override
                        public void onResult(TurnBasedMultiplayer.InitiateMatchResult result) {
                            processResult(result);
                        }
                    });
            showSpinner();
        }
    }



    /*
        La méthode startMatch() est appelée en réponse (par callback)
        de l'appel de création d'un match. Puisqu'elle a été appelée
        seulement lor d'un succès, l'objet de match devrait être valide.
        La partie est mise dans son état initial, puis takeTurn() est appelée.
        L'appel de takeTurn() appelle la méthode processResult() correspondante
    */
    public void startMatch(TurnBasedMatch match) {

        mTurnData = new GameBoard();
        mTurnData.turnCounter = 1;
        mTurnData.isWhiteTurn = true;
        mTurnData.encodeFromBoardToData();

        mMatch = match;

        String playerId = Games.Players.getCurrentPlayerId(mGoogleApiClient);
        String myParticipantId = mMatch.getParticipantId(playerId);

        disableMyTwoButtons();
        enableForfeitButton();
        askForRematchButton.setClickable(false);
        askForRematchButton.setVisibility(View.GONE);

        showSpinner();

        Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, match.getMatchId(),
                mTurnData.persist(), myParticipantId).setResultCallback(
                new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                        processResult(result);
                    }
                });
    }

    /*
        Méthode appelée pour débuter un nouveau match avec le même adversaire.
        Appelée s'il y a confirmation dans une boîte de dialogue après un clic sur
        le bouton askForRematchButton (si bien sûr ce bouton était activé)
     */
    public void rematch() {
        showSpinner();
        Games.TurnBasedMultiplayer.rematch(mGoogleApiClient, mMatch.getMatchId()).setResultCallback(
                new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.InitiateMatchResult result) {
                        processResult(result);
                    }
                });
        mMatch = null;
        showTheGameplay = false;
    }

    /*
     * Renvoie le ID du prochain joueur
     * @return participantId du joueur qui suit au tour à tour, nul si c'est un automatch
     */
    public String getNextParticipantId() {

        String playerId = Games.Players.getCurrentPlayerId(mGoogleApiClient);
        String myParticipantId = mMatch.getParticipantId(playerId);

        ArrayList<String> participantIds = mMatch.getParticipantIds();

        int desiredIndex = -1;

        for (int i = 0; i < participantIds.size(); i++) {
            if (participantIds.get(i).equals(myParticipantId)) {
                desiredIndex = i + 1;
            }
        }

        if (desiredIndex < participantIds.size()) {
            return participantIds.get(desiredIndex);
        }

        if (mMatch.getAvailableAutoMatchSlots() <= 0) {
            // You've run out of automatch slots, so we start over.
            return participantIds.get(0);
        } else {
            // You have not yet fully automatched, so null will find a new
            // person to play against.
            return null;
        }
    }

    // // // // // // // // // // // // //
    // Fonction extrêmement importante //
    // // // // // // // // // // // //

    /*
        Méthode de mise-à-jour de la partie. Selon si la partie est sur le
        point d'être complétée ou non, et selon le tour du joueur si elle ne
        l'est pas, cette méthode présente une vue légèrement différente de
        l'écran de la partie au joueur.
     */
    public void updateMatch(TurnBasedMatch match) {
        mMatch = match;
        int status = match.getStatus();
        int turnStatus = match.getTurnStatus();

        switch (status) {
            case TurnBasedMatch.MATCH_STATUS_CANCELED:
                showWarning("Annulée!", "Cette partie a été annulée!");
                return;
            case TurnBasedMatch.MATCH_STATUS_EXPIRED:
                showWarning("Expirée!", "Cette partie a expiré!");
                return;
            case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
                showWarning("Waiting for auto-match...",
                        "We're still waiting for an automatch partner.");
                return;
            case TurnBasedMatch.MATCH_STATUS_COMPLETE:
                //Si elle a été déclarée comme terminée par les deux joueurs
                if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE)
                {
                    showWarning( "Terminée!",  "Cette partie est finie.");
                }
                //si elle a été déclarée terminée par un seul des deux joueurs
                else
                {
                    setFinalTurn();
                }
                return;
        }
        //À partir d'ici, la partie n'a été déclarée terminée par personne
        switch (turnStatus) {
            case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
                myTurn = true;
                setTurn();
                setGameplayUI(whiteTurn);
                setViewVisibility();
                return;

            case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN:
                myTurn = false;
                setTurn();
                setGameplayUI(!whiteTurn);
                setViewVisibility();
                return;

            case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
                showWarning("En attente", "En attente d'autres invitations");
                mTurnData = null;
                setViewVisibility();
                break;

            default:
                mTurnData = null;
                setViewVisibility();
                break;
        }
    }

    /*
        Méthode utilitaire qui vérifie s'il y a un éche ou non sur le roi de la couleur demandée
     */
    private boolean thereIsACheck(boolean onWhiteKing){
        boolean retVal;
        if(onWhiteKing){
            retVal =  mTurnData.whiteKingIsInCheck();
        }
        else{
            retVal = mTurnData.blackKingIsInCheck();
        }
        return retVal;
    }

    /*
        Appelée par updateMatch() à chaque tour qui n'est pas un tour final
     */
    private void setTurn(){
        mTurnData = GameBoard.unpersist(mMatch.getData());
        mTurnData.decodeFromDataToBoard();
        whiteTurn = mTurnData.isWhiteTurn;
        proposedMove = "";
        hasAMoveToUndo = false;
    }

    /*
        Appelée par updateMatch() pour le joueur qui n'a pas fait le dernier coup
    */
    private void setFinalTurn(){
        disableMyTwoButtons();
        disableForfeitButton();
        mTurnData = GameBoard.unpersist(mMatch.getData());
        mTurnData.decodeFromDataToBoard();
        whiteTurn = mTurnData.isWhiteTurn;
        showTurn.setTextColor(Color.parseColor("#ffe341ff"));
        showTurnColor.setTextColor(Color.parseColor("#ffe341ff"));
        if(mTurnData.winner == -1){
            showEndMessage("Pat", "Partie nulle");
            showTurn.setText("Pat, partie nulle");
            showTurnColor.setText("  ");
        }
        else if(mTurnData.winner == 0){
            checkWarning.setVisibility(View.VISIBLE);
            matWarning.setVisibility(View.VISIBLE);
            if(mTurnData.forfeit){
                showEndMessage("Par Abandon", "Vous avez gagné");
                showTurn.setText(" Abandon de l'adversaire");}
            else{
                showEndMessage("Échec et mat", "Les Blancs ont gagné");
                showTurn.setText("Échec et mat!");
            }
            showTurnColor.setText("Les Blancs ont gagné");
        }
        else if(mTurnData.winner == 1){
            checkWarning.setVisibility(View.VISIBLE);
            matWarning.setVisibility(View.VISIBLE);
            if(mTurnData.forfeit){
                showEndMessage("Par Abandon", "Vous avez gagné");
                showTurn.setText(" Abandon de l'adversaire");}
            else{
                showEndMessage("Échec et mat", "Les Noirs ont gagné");
                showTurn.setText("Échec et mat!");
            }
            showTurnColor.setText("Les Noirs ont gagné");
        }
        myTurn = false;
        showTheGameplay = true;
        if(mTurnData.isWhiteTurn) {
            setNotation(true);
            mTurnData.displayForWhite();
        }
        else {
            setNotation(false);
            mTurnData.displayForBlack();
        }
        setViewVisibility();

        mustEnd = true;

    }

    /*
        Méthode servant à envoyer un message de fin
     */

    private void showEndMessage(String title, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle(title).setMessage(message);

        // set dialog message
        alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, close
                        // current activity
                        dialog.dismiss();
                    }
                });
        mAlertDialog = alertDialogBuilder.create();
        mAlertDialog.show();
    }


    // /// // // // / // /// / / ///
    //  Les cinq processResult() ///
    // // / //// /  ///// //// / //


    /*
        Les cinq méthodes suivantes sont très importantes. Elles sont un point d'articulation
        entre tous les callbacks de l'API du genre .setActivityForResult.

     */
    private void processResult(TurnBasedMultiplayer.CancelMatchResult result) {
        dismissSpinner();

        if (!checkStatusCode(null, result.getStatus().getStatusCode())) {
            return;
        }

        showTheGameplay = false;

        showWarning("Match",
                "Partie annulée.");
    }

    private void processResult(TurnBasedMultiplayer.InitiateMatchResult result) {
        TurnBasedMatch match = result.getMatch();
        dismissSpinner();

        if (!checkStatusCode(match, result.getStatus().getStatusCode())) {
            return;
        }

        if (match.getData() != null) {
            // This is a game that has already started, so I'll just start
            updateMatch(match);
            return;
        }

        startMatch(match);
    }


    private void processResult(TurnBasedMultiplayer.LeaveMatchResult result) {
        TurnBasedMatch match = result.getMatch();
        dismissSpinner();
        if (!checkStatusCode(match, result.getStatus().getStatusCode())) {
            return;
        }
        showTheGameplay = (match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN);
        showWarning("Left", "You've left this match.");
    }


    public void processResult(TurnBasedMultiplayer.UpdateMatchResult result) {
        Log.d("PROCESS", "UpateMatchResult obtenu");
        TurnBasedMatch match = result.getMatch();
        dismissSpinner();
        if (!checkStatusCode(match, result.getStatus().getStatusCode())) {
            return;
        }
        if (match.canRematch()) {
            askForRematchButton.setClickable(true);
            askForRematchButton.setVisibility(View.VISIBLE);
        }

        if(match.getStatus() != TurnBasedMatch.MATCH_STATUS_COMPLETE) {

            if(showTheGameplay){
                updateMatch(match);
                return;
            }
            else {
                //Que ce soit notre tour ou celui de l'adversaire, on appelle updateMatch sur le match
                showTheGameplay = (match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN ||
                        match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN);
                if (showTheGameplay) {
                    updateMatch(match);
                    return;
                }
            }
        }


        setViewVisibility();
    }


    private void processResult(TurnBasedMultiplayer.LoadMatchesResult result) {
        Log.d("Process LoadM", "On rentre dans le bon processResult");
        if(result.getStatus().isSuccess()){
            LoadMatchesResponse response = result.getMatches()  ;
            TurnBasedMatchBuffer myBuffer = response.getMyTurnMatches();
            int n = myBuffer.getCount();
            if(n>0)
                checkGamesButton.setText("Mes parties (" + n + "!)");
            else
                checkGamesButton.setText("Mes parties");
        }
        dismissSpinner();
        return;
    }


    // // // // // // // // // // // // // // // // // // // // // // //
    // Gestion des toasts par l'impantation des méthodes des listeners //
    // // // // // // // // // // // // // // // // // // // // // // //

    /*
        Les quatre méthodes suivantes sont implantées pour l'obtention de notifications ou
        de toasts concernant les invitations à jouer et les tours pris dans les parties
     */

    @Override
    public void onInvitationReceived(Invitation invitation) {
        Toast toast = Toast.makeText(
                this,
                invitation.getInviter().getDisplayName() + " vous invite à jouer une partie.", TOAST_DELAY);
        toast.setGravity(Gravity.TOP| Gravity.LEFT, 0, 0);
        toast.show();
    }

    @Override
    public void onInvitationRemoved(String invitationId) {
        Toast toast = Toast.makeText(this, "Une invitation a été annulée.", TOAST_DELAY);
        toast.setGravity(Gravity.TOP| Gravity.LEFT, 0, 0);
        toast.show();
    }


    @Override
    public void onTurnBasedMatchReceived(TurnBasedMatch match) {
        Log.d("NOTIFICATION ", "TurnBasedMatch Received. Toast");
        if(mMatch.getMatchId().equals(match.getMatchId()) &&
                 match.getStatus() == TurnBasedMatch.MATCH_STATUS_ACTIVE &&
                     match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN &&
                        GameBoard.unpersist(mMatch.getData()).gameOver == false && showTheGameplay)
        {
            Toast toast = Toast.makeText(this, "Votre adversaire a joué, c'est à vous.", TOAST_DELAY);
            toast.setGravity(Gravity.TOP | Gravity.LEFT, 0, 0);
            toast.show();
            updateMatch(match);
        }
        else if (mMatch.getMatchId().equals(match.getMatchId()) &&
                    match.getStatus() != TurnBasedMatch.MATCH_STATUS_ACTIVE &&
                        match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN )
        {
            updateMatch(match);
        }
        else if (!mMatch.getMatchId().equals( match.getMatchId())  ||
                (mMatch.getMatchId().equals( match.getMatchId()) && !showTheGameplay)  )
        {
            Toast toast = Toast.makeText(this, "Une partie a été modifiée", TOAST_DELAY);
            toast.setGravity(Gravity.TOP | Gravity.LEFT, 0, 0);
            toast.show();
        }
        else if (!mMatch.getMatchId().equals(match.getMatchId())  ||
                (mMatch.getMatchId().equals(match.getMatchId()) && !showTheGameplay)  )
        {
            updateMyTurnMatchesNumber();
            Toast toast = Toast.makeText(this, "Une partie a été modifiée", TOAST_DELAY);
            toast.setGravity(Gravity.TOP| Gravity.LEFT, 0, 0);
            toast.show();
        }
    }

    @Override
    public void onTurnBasedMatchRemoved(String matchId) {
        Toast toast = Toast.makeText(this, "Une partie a été retirée.", TOAST_DELAY);
        toast.setGravity(Gravity.TOP| Gravity.LEFT, 0, 0);
        toast.show();

    }



    public void showErrorMessage(TurnBasedMatch match, int statusCode,
                                 int stringId) {

        showWarning("Attention", getResources().getString(stringId));
    }

    /*
        Méthode de test de status de connexion
     */
    private boolean checkStatusCode(TurnBasedMatch match, int statusCode) {
        switch (statusCode) {
            case GamesStatusCodes.STATUS_OK:
                return true;
            case GamesStatusCodes.STATUS_NETWORK_ERROR_OPERATION_DEFERRED:
                // C'est ok, l'action sera lente mais sera prise en charge
                return true;
            case GamesStatusCodes.STATUS_MULTIPLAYER_ERROR_NOT_TRUSTED_TESTER:
                showErrorMessage(match, statusCode,
                        R.string.status_multiplayer_error_not_trusted_tester);
                break;
            case GamesStatusCodes.STATUS_MATCH_ERROR_ALREADY_REMATCHED:
                showErrorMessage(match, statusCode,
                        R.string.match_error_already_rematched);
                break;
            case GamesStatusCodes.STATUS_NETWORK_ERROR_OPERATION_FAILED:
                showErrorMessage(match, statusCode,
                        R.string.network_error_operation_failed);
                break;
            case GamesStatusCodes.STATUS_CLIENT_RECONNECT_REQUIRED:
                showErrorMessage(match, statusCode,
                        R.string.client_reconnect_required);
                break;
            case GamesStatusCodes.STATUS_INTERNAL_ERROR:
                showErrorMessage(match, statusCode, R.string.internal_error);
                break;
            case GamesStatusCodes.STATUS_MATCH_ERROR_INACTIVE_MATCH:
                showErrorMessage(match, statusCode,
                        R.string.match_error_inactive_match);
                break;
            case GamesStatusCodes.STATUS_MATCH_ERROR_LOCALLY_MODIFIED:
                showErrorMessage(match, statusCode,
                        R.string.match_error_locally_modified);
                break;
            default:
                showErrorMessage(match, statusCode, R.string.unexpected_status);
                Log.d(TAG, "Aucun message de warning n'a été écrit "
                        + statusCode);
        }

        return false;
    }


    // // / / // // // // // // // /// // // // // //
    // Méthodes de gestion de l'échiquier graphique //
    // // // // // // // // // // // // // // // // //

    /*
        Gère les clics sur les boutons de sign-in et de sign-out ainsi que sur l'échiquier

     */

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                mSignInClicked = true;
                mTurnBasedMatch = null;
                findViewById(R.id.sign_in_button).setVisibility(View.GONE);
                mGoogleApiClient.connect();
                break;
            case R.id.sign_out_button:
                mSignInClicked = false;
                Games.signOut(mGoogleApiClient);
                if (mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.disconnect();
                }
                setViewVisibility();
                break;
            default:
                if(myTurn && !hasAMoveToUndo && mTurnData != null)
                    onChessBoardClick(v);
                break;
        }
    }



    private boolean playerCanMakeAMove(GameBoard.PlayerColor color) {
        return !mTurnData.giveAllPossibleMoves(color).isEmpty();
    }


    // // // // // // // // // // // // // // // // / / /// /// // // ////  ////// ///
    // Les méthodes qui suivent concernent l'interaction avec l'échiquier graphique //
    // // // // // // //  // // // // // // // // // // // // // // // / // // // // /


    /*
        Fonction extrêmement importante.
        Cette méthode gère les clics sur l'échiquier graphique.
        Il faut minimum deux clic pour jouer un coup : le premier pour sélectionner
        une pièce, le deuxième pour sélectionner la case de destination.

        Cette méthode utilise énormément les méthodes de la classe GameBoard. Il s'agit
        de la traduction des deux clics de l'utilisateur dans un encodage de coup et de la
        recherche de coup-candidat dans la banque de coups retourné par giveAllPossibleMoves()
     */

    private void onChessBoardClick(View v){
        Log.d("Clic", "SUR LE CHESSBOARD");
        int r = -1;
        int c = -1;
        String entryId = v.getResources().getResourceEntryName(v.getId());
        if(whiteTurn) {
            r = 8 - Character.getNumericValue(entryId.charAt(1));
            c = entryId.charAt(0) - 'a';
        }
        else if (!whiteTurn){
            r = 7 - (8 - Character.getNumericValue(entryId.charAt(1)));
            c = 7 - (entryId.charAt(0) - 'a');
        }
        //Si aucune pièce n'est déjà sélectionnée
        if(!aPieceIsAlreadySelected){
            //et si le joueur clique sur une de ses pièces, c'est une sélection
            if( (whiteTurn && mTurnData.isAWhitePiece(r,c)) || (!whiteTurn && mTurnData.isABlackPiece(r,c)) ){
                if(whiteTurn)
                    drawPieceAsSelectedForWhite(r, c);
                else
                    drawPieceAsSelectedForBlack(r,c);
                drawPossibleSquaresAsSelected(r, c);
                rowOfPieceSelected = r;
                columnOfPieceSelected = c;
                aPieceIsAlreadySelected = true;
                Log.d("Selection de: ", "" + mTurnData.getPiece(r, c) + " en " + "(" + r + "," + c + ")");
            }
        }
        //Si une pièce est déjà sélectionnée
        else if (aPieceIsAlreadySelected){

            String attemptedMove = "" + rowOfPieceSelected + "" + columnOfPieceSelected + "" + r + "" + c + "" + mTurnData.getPiece(r, c);
            mTurnData.resetColorOfSquares();
            if(whiteTurn)
                redrawSelectedPieceAsStandard(true);
            else
                redrawSelectedPieceAsStandard(false);
            //et si le joueur clique sur une de ses pièces, c'est une déselection
            if(whiteTurn && mTurnData.isAWhitePiece(r,c)){
                redrawSelectedPieceAsStandard(true);
                aPieceIsAlreadySelected = false;
            }else if(!whiteTurn && mTurnData.isABlackPiece(r,c)){
                redrawSelectedPieceAsStandard(false);
                aPieceIsAlreadySelected = false;
            }
            //sinon, si le joueur est blanc et qu'il clique sur une case vide ou sur une pièce adverse
            else if (whiteTurn && ( mTurnData.isABlank(r,c) || mTurnData.isABlackPiece(r,c) )) {

                //si le premier clic était sur le roi blanc (7,4) et le second clic est sur la case (7,6)
                if(mTurnData.getPiece(Character.getNumericValue(attemptedMove.charAt(0)),Character.getNumericValue(attemptedMove.charAt(1))) == GameBoard.WHITE_KING &&
                        attemptedMove.charAt(0) == '7' && attemptedMove.charAt(1) == '4' &&
                            attemptedMove.charAt(2) == '7' && attemptedMove.charAt(3) == '6')
                {

                    Log.d("ChessBoClick","petit roque move");
                    if(mTurnData.giveAllPossibleMoves(GameBoard.PlayerColor.WHITE).contains("WCKS")){
                        mTurnData.makeAMove("WCKS");
                        mTurnData.displayForWhite();
                        aPieceIsAlreadySelected = false;
                        proposedMove = "WCKS";
                        hasAMoveToUndo = true;
                        enableMyTwoButtons();
                        Log.d("Coup joué: ", "de" + "(" + rowOfPieceSelected + "," + columnOfPieceSelected + ")" + " vers " + "(" + r + "," + c + ")");
                        //changeTurn();
                    }
                }
                //si le premier clic était sur le roi blanc (7,4) et le second clic est sur la case (7,2)
                else if(mTurnData.getPiece(Character.getNumericValue(attemptedMove.charAt(0)),Character.getNumericValue(attemptedMove.charAt(1))) == GameBoard.WHITE_KING &&
                        attemptedMove.charAt(0) == '7' && attemptedMove.charAt(1) == '4' &&
                            attemptedMove.charAt(2) == '7' && attemptedMove.charAt(3) == '2')
                {
                    Log.d("ChessBoClick","grand roque move");
                    if(mTurnData.giveAllPossibleMoves(GameBoard.PlayerColor.WHITE).contains("WCQS")){
                        mTurnData.makeAMove("WCQS");
                        mTurnData.displayForWhite();
                        aPieceIsAlreadySelected = false;
                        proposedMove = "WCQS";
                        hasAMoveToUndo = true;
                        enableMyTwoButtons();
                        Log.d("Coup joué: ", "de" + "(" + rowOfPieceSelected + "," + columnOfPieceSelected + ")" + " vers " + "(" + r + "," + c + ")");
                        //changeTurn();
                    }
                }
                //si le premier clic était sur un pion blanc de rangée 3 et le second clic est sur la case (2,c)
                //et que la case (2,c) contient une case vide que (2,c) est en diagonale directe avec la case d'origine
                else if (mTurnData.getPiece(Character.getNumericValue(attemptedMove.charAt(0)),Character.getNumericValue(attemptedMove.charAt(1))) == GameBoard.WHITE_PAWN &&
                            attemptedMove.charAt(0) == '3' && attemptedMove.charAt(2) == '2' && mTurnData.getPiece(r,c) == GameBoard.EMPTY_SQUARE &&
                                (Character.getNumericValue(attemptedMove.charAt(3)) == Character.getNumericValue(attemptedMove.charAt(1)) + 1 ||
                                    Character.getNumericValue(attemptedMove.charAt(3)) == Character.getNumericValue(attemptedMove.charAt(1)) -1) )
                {
                    Log.d("ChessBoClick","EnPassant move");
                    Log.d("EnPassant", "Premier if franchi");
                    String s = "EP3"+attemptedMove.charAt(1) + attemptedMove.charAt(2) + attemptedMove.charAt(3) + GameBoard.BLACK_PAWN;
                    if(mTurnData.giveAllPossibleMoves(GameBoard.PlayerColor.WHITE).contains(s))
                    {
                        Log.d("EnPassant", "Deuxieme if franchi");
                        proposedMove = s;
                        mTurnData.makeAMove(proposedMove);
                        mTurnData.displayForWhite();
                        aPieceIsAlreadySelected = false;
                        hasAMoveToUndo = true;
                        enableMyTwoButtons();
                        Log.d("Coup joué: ", "de" + "(" + rowOfPieceSelected + "," + columnOfPieceSelected + ")" + " vers " + "(" + r + "," + c + ")");
                        //changeTurn();
                    }
                }
                //promotion
                //si le premier clic était sur un pion blanc de rangée 1 et que le second est sur une pièce de rangée 0 et de colonne adjacente
                else if(mTurnData.getPiece(rowOfPieceSelected,columnOfPieceSelected) == GameBoard.WHITE_PAWN &&
                         rowOfPieceSelected == 1 && r == 0  &&  ( c == columnOfPieceSelected -1  ||
                            c ==  columnOfPieceSelected  ||  c == columnOfPieceSelected +1  ) )
                {
                    Log.d("ChessBoClick","promotion move");
                    String s = "" + 'X' + rowOfPieceSelected + columnOfPieceSelected + r + c + mTurnData.getPiece(r,c) + 'Q';
                    if(mTurnData.giveAllPossibleMoves(GameBoard.PlayerColor.WHITE).contains(s)){
                        Log.d("Promotion", "Deuxieme if franchi");
                        proposedMove = s;
                        showPromotionSelection();
                    }

                }

                //sinon
                else if (mTurnData.giveAllPossibleMoves(GameBoard.PlayerColor.WHITE).contains(attemptedMove))
                {
                    Log.d("ChessBoClick","general move");
                    mTurnData.makeAMove(attemptedMove);
                    mTurnData.displayForWhite();
                    aPieceIsAlreadySelected = false;
                    proposedMove = attemptedMove;
                    hasAMoveToUndo = true;
                    enableMyTwoButtons();
                    Log.d("Coup joué: ", "de" + "(" + rowOfPieceSelected + "," + columnOfPieceSelected + ")" + " vers " + "(" + r + "," + c + ")");
                    //changeTurn();
                }

            }

            //si le joueur est noir et qu'il clique sur une case vide ou sur une pièce adverse
            else if (!whiteTurn && ( mTurnData.isABlank(r,c) || mTurnData.isAWhitePiece(r,c) )) {

                //si le premier clic était sur le roi noir en (0,4) et le second clic est sur la case (0,6)
                if(mTurnData.getPiece(Character.getNumericValue(attemptedMove.charAt(0)),Character.getNumericValue(attemptedMove.charAt(1))) == GameBoard.BLACK_KING &&
                        attemptedMove.charAt(0) == '0' && attemptedMove.charAt(1) == '4' &&
                            attemptedMove.charAt(2) == '0' && attemptedMove.charAt(3) == '6')
                {
                    if(mTurnData.giveAllPossibleMoves(GameBoard.PlayerColor.BLACK).contains("BCKS")){
                        mTurnData.makeAMove("BCKS");
                        mTurnData.displayForBlack();
                        aPieceIsAlreadySelected = false;
                        proposedMove = "BCKS";
                        hasAMoveToUndo = true;
                        enableMyTwoButtons();
                        Log.d("Coup joué: ", "de" + "(" + rowOfPieceSelected + "," + columnOfPieceSelected + ")" + " vers " + "(" + r + "," + c + ")");
                        //changeTurn();
                    }
                }
                //si le premier clic était sur le roi noir  en (0,4) et le second clic est sur la case (0,2)
                else if(mTurnData.getPiece(Character.getNumericValue(attemptedMove.charAt(0)),Character.getNumericValue(attemptedMove.charAt(1))) == GameBoard.BLACK_KING &&
                        attemptedMove.charAt(0) == '0' && attemptedMove.charAt(1) == '4' &&
                            attemptedMove.charAt(2) == '0' && attemptedMove.charAt(3) == '2')
                {
                    if(mTurnData.giveAllPossibleMoves(GameBoard.PlayerColor.BLACK).contains("BCQS")){
                        mTurnData.makeAMove("BCQS");
                        mTurnData.displayForBlack();
                        aPieceIsAlreadySelected = false;
                        proposedMove = "BCQS";
                        hasAMoveToUndo = true;
                        enableMyTwoButtons();
                        Log.d("Coup joué: ", "de" + "(" + rowOfPieceSelected + "," + columnOfPieceSelected + ")" + " vers " + "(" + r + "," + c + ")");
                        //changeTurn();
                    }
                }
                //si le premier clic était sur un pion noir de rangée 4 et le second clic est sur la rangée 5
                //et que la case (r,c) contient une case vide que (5,c) est en diagonale directe avec la case d'origine
                else if (mTurnData.getPiece(Character.getNumericValue(attemptedMove.charAt(0)),Character.getNumericValue(attemptedMove.charAt(1))) == GameBoard.BLACK_PAWN &&
                        attemptedMove.charAt(0) == '4' && attemptedMove.charAt(2) == '5' && mTurnData.getPiece(r,c) == GameBoard.EMPTY_SQUARE &&
                        (Character.getNumericValue(attemptedMove.charAt(3)) == Character.getNumericValue(attemptedMove.charAt(1)) + 1 ||
                                Character.getNumericValue(attemptedMove.charAt(3)) == Character.getNumericValue(attemptedMove.charAt(1)) -1) )
                {
                    String s = "EP4"+attemptedMove.charAt(1) + attemptedMove.charAt(2) + attemptedMove.charAt(3) + GameBoard.WHITE_PAWN;
                    if(mTurnData.giveAllPossibleMoves(GameBoard.PlayerColor.BLACK).contains(s))
                    {
                        proposedMove = s;
                        mTurnData.makeAMove(proposedMove);
                        mTurnData.displayForBlack();
                        aPieceIsAlreadySelected = false;
                        hasAMoveToUndo = true;
                        enableMyTwoButtons();
                        Log.d("Coup joué: ", "de" + "(" + rowOfPieceSelected + "," + columnOfPieceSelected + ")" + " vers " + "(" + r + "," + c + ")");
                        //changeTurn();
                    }
                }


                //promotion
                //si le premier clic était sur un pion noire de rangée 6 et que le second est sur une pièce de rangée 7 et de colonne adjacente
                else if(mTurnData.getPiece(rowOfPieceSelected,columnOfPieceSelected) == GameBoard.BLACK_PAWN &&
                        rowOfPieceSelected == 6 && r == 7  &&  ( c == columnOfPieceSelected -1  ||
                        c ==  columnOfPieceSelected  ||  c == columnOfPieceSelected +1  ) )
                {
                    Log.d("ChessBoClick","promotion move");
                    String s = "" + 'x' + rowOfPieceSelected + columnOfPieceSelected + r + c + mTurnData.getPiece(r,c) + 'q';
                    if(mTurnData.giveAllPossibleMoves(GameBoard.PlayerColor.BLACK).contains(s)){
                        Log.d("Promotion", "Deuxieme if franchi");
                        proposedMove = s;
                        showPromotionSelection();
                    }

                }


                //sinon
                else if(mTurnData.giveAllPossibleMoves(GameBoard.PlayerColor.BLACK).contains(attemptedMove)){
                    mTurnData.makeAMove(attemptedMove);
                    mTurnData.displayForBlack();
                    aPieceIsAlreadySelected = false;
                    proposedMove = attemptedMove;
                    hasAMoveToUndo = true;
                    enableMyTwoButtons();
                    Log.d("Coup joué: ", "de" + "(" + rowOfPieceSelected + "," + columnOfPieceSelected + ")" + " vers " + "(" + r + "," + c + ")");
                    //changeTurn();
                }

            }

        }
    }

    /*
        Méthode qui allume les cases de destination possible d'une pièce sélectionnée
     */

    private void drawPossibleSquaresAsSelected(int r, int c) {
        HashSet<String> possibleMoves;
        if(whiteTurn)
            possibleMoves = mTurnData.giveAllPossibleMoves(GameBoard.PlayerColor.WHITE);
        else
            possibleMoves = mTurnData.giveAllPossibleMoves(GameBoard.PlayerColor.BLACK);

        for(String move : possibleMoves)
        {
            if(Character.getNumericValue(move.charAt(0)) == r && Character.getNumericValue(move.charAt(1)) == c
                    && move.charAt(0)!='X' && move.charAt(0)!='x' && Character.isDigit(move.charAt(1)) &&
                        Character.isDigit(move.charAt(2)))
            {
                if((Character.getNumericValue(move.charAt(2)) + Character.getNumericValue(move.charAt(3))) % 2 == 0)
                {
                    if(whiteTurn)
                        GameBoard.gameImageViews[Character.getNumericValue(move.charAt(2))][Character.getNumericValue(move.charAt(3))].setBackgroundColor(Color.LTGRAY);
                    else
                        GameBoard.gameImageViews[7-Character.getNumericValue(move.charAt(2))][7-Character.getNumericValue(move.charAt(3))].setBackgroundColor(Color.LTGRAY);

                }
                else
                {
                    if(whiteTurn)
                        GameBoard.gameImageViews[Character.getNumericValue(move.charAt(2))][Character.getNumericValue(move.charAt(3))].setBackgroundColor(Color.DKGRAY);
                    else
                        GameBoard.gameImageViews[7-Character.getNumericValue(move.charAt(2))][7-Character.getNumericValue(move.charAt(3))].setBackgroundColor(Color.DKGRAY);

                }
            }
            //si c'est un roi blanc
            if(mTurnData.getPiece(r,c) == GameBoard.WHITE_KING){
                if(move.charAt(0) == 'W' && move.charAt(1) == 'C' && move.charAt(2) == 'K'){
                    GameBoard.gameImageViews[7][5].setBackgroundColor(Color.LTGRAY);
                    GameBoard.gameImageViews[7][6].setBackgroundColor(Color.DKGRAY);
                }
                else if(move.charAt(0) == 'W' && move.charAt(1) == 'C' && move.charAt(2) == 'Q'){
                    GameBoard.gameImageViews[7][3].setBackgroundColor(Color.LTGRAY);
                    GameBoard.gameImageViews[7][2].setBackgroundColor(Color.DKGRAY);
                }
            }
            //si c'est un roi noir
            if(mTurnData.getPiece(r,c) == GameBoard.BLACK_KING){
                if(move.charAt(0) == 'B' && move.charAt(1) == 'C' && move.charAt(2) == 'K'){
                    GameBoard.gameImageViews[7-0][7-5].setBackgroundColor(Color.DKGRAY);
                    GameBoard.gameImageViews[7-0][7-6].setBackgroundColor(Color.LTGRAY);
                }
                else if(move.charAt(0) == 'B' && move.charAt(1) == 'C' && move.charAt(2) == 'Q'){
                    GameBoard.gameImageViews[7-0][7-3].setBackgroundColor(Color.DKGRAY);
                    GameBoard.gameImageViews[7-0][7-2].setBackgroundColor(Color.LTGRAY);
                }
            }
            //si c'est un pion blanc dans la rangée 3
            if(mTurnData.getPiece(r,c) == GameBoard.WHITE_PAWN && r == 3){
                if(move.charAt(0) == 'E' && move.charAt(1) == 'P' &&  move.charAt(2) == '3' &&
                        Character.getNumericValue(move.charAt(3)) == c)
                {
                    if((Character.getNumericValue(move.charAt(4)) + Character.getNumericValue(move.charAt(5))) % 2 == 0)
                    {
                        GameBoard.gameImageViews[Character.getNumericValue(move.charAt(4))][Character.getNumericValue(move.charAt(5))].setBackgroundColor(Color.LTGRAY);
                    }
                    else
                    {
                        GameBoard.gameImageViews[Character.getNumericValue(move.charAt(4))][Character.getNumericValue(move.charAt(5))].setBackgroundColor(Color.DKGRAY);
                    }
                }
            }
            //si c'est un pion noir dans la rangée 4
            if(mTurnData.getPiece(r,c) == GameBoard.BLACK_PAWN && r == 4){
                if(move.charAt(0) == 'E' && move.charAt(1) == 'P' && move.charAt(2) == '4' &&
                        Character.getNumericValue(move.charAt(3)) == c)
                {
                    if((Character.getNumericValue(move.charAt(4)) + Character.getNumericValue(move.charAt(5))) % 2 == 0)
                    {
                        GameBoard.gameImageViews[7-Character.getNumericValue(move.charAt(4))][7-Character.getNumericValue(move.charAt(5))].setBackgroundColor(Color.LTGRAY);
                    }
                    else
                    {
                        GameBoard.gameImageViews[7-Character.getNumericValue(move.charAt(4))][7-Character.getNumericValue(move.charAt(5))].setBackgroundColor(Color.DKGRAY);
                    }
                }
            }

            //si c'est une possible promotion blanche
            if(mTurnData.getPiece(r,c) == GameBoard.WHITE_PAWN && r == 1){
                if(move.charAt(0) == 'X'){
                    if((Character.getNumericValue(move.charAt(3)) + Character.getNumericValue(move.charAt(4))) % 2 == 0)
                    {
                        GameBoard.gameImageViews[Character.getNumericValue(move.charAt(3))][Character.getNumericValue(move.charAt(4))].setBackgroundColor(Color.LTGRAY);
                    }
                    else
                    {
                        GameBoard.gameImageViews[Character.getNumericValue(move.charAt(3))][Character.getNumericValue(move.charAt(4))].setBackgroundColor(Color.DKGRAY);
                    }

                }
            }

            //si c'est une possible promotion noire
            if(mTurnData.getPiece(r,c) == GameBoard.BLACK_PAWN && r == 6){
                if(move.charAt(0) == 'x'){
                    if((Character.getNumericValue(move.charAt(3)) + Character.getNumericValue(move.charAt(4))) % 2 == 0)
                    {
                        GameBoard.gameImageViews[7-Character.getNumericValue(move.charAt(3))][7-Character.getNumericValue(move.charAt(4))].setBackgroundColor(Color.LTGRAY);
                    }
                    else
                    {
                        GameBoard.gameImageViews[7-Character.getNumericValue(move.charAt(3))][7-Character.getNumericValue(move.charAt(4))].setBackgroundColor(Color.DKGRAY);
                    }

                }
            }




        }

    }

    /*
        Méthode qui dessine la pièce sélectionnée en vert. Du point de vue des blancs
     */
    private void drawPieceAsSelectedForWhite(int r, int c) {
        char piece = mTurnData.getPiece(r, c);
        if(piece == GameBoard.WHITE_ROOK){
            GameBoard.gameImageViews[r][c].setImageResource(R.drawable.white_rook_selected);
            idLastSelectedRessource = (R.drawable.white_rook);
        }else if (piece == GameBoard.WHITE_KNIGHT){
            GameBoard.gameImageViews[r][c].setImageResource(R.drawable.white_knight_selected);
            idLastSelectedRessource = (R.drawable.white_knight);
        }else if (piece == GameBoard.WHITE_BISHOP){
            GameBoard.gameImageViews[r][c].setImageResource(R.drawable.white_bishop_selected);
            idLastSelectedRessource = (R.drawable.white_bishop);
        }else if (piece == GameBoard.WHITE_QUEEN){
            GameBoard.gameImageViews[r][c].setImageResource(R.drawable.white_queen_selected);
            idLastSelectedRessource = (R.drawable.white_queen);
        }else if (piece == GameBoard.WHITE_KING){
            GameBoard.gameImageViews[r][c].setImageResource(R.drawable.white_king_selected);
            idLastSelectedRessource = (R.drawable.white_king);
        }else if (piece == GameBoard.WHITE_PAWN){
            GameBoard.gameImageViews[r][c].setImageResource(R.drawable.white_pawn_selected);
            idLastSelectedRessource = (R.drawable.white_pawn);
        }
        else if (piece == GameBoard.BLACK_ROOK){
            GameBoard.gameImageViews[r][c].setImageResource(R.drawable.black_rook_selected);
            idLastSelectedRessource = (R.drawable.black_rook);
        }else if (piece == GameBoard.BLACK_KNIGHT){
            GameBoard.gameImageViews[r][c].setImageResource(R.drawable.black_knight_selected);
            idLastSelectedRessource = (R.drawable.black_knight);
        }else if (piece == GameBoard.BLACK_BISHOP){
            GameBoard.gameImageViews[r][c].setImageResource(R.drawable.black_bishop_selected);
            idLastSelectedRessource = (R.drawable.black_bishop);
        }else if (piece == GameBoard.BLACK_QUEEN){
            GameBoard.gameImageViews[r][c].setImageResource(R.drawable.black_queen_selected);
            idLastSelectedRessource = (R.drawable.black_queen);
        }else if (piece == GameBoard.BLACK_KING){
            GameBoard.gameImageViews[r][c].setImageResource(R.drawable.black_king_selected);
            idLastSelectedRessource = (R.drawable.black_king);
        }else if (piece == GameBoard.BLACK_PAWN){
            GameBoard.gameImageViews[r][c].setImageResource(R.drawable.white_pawn_selected);
            idLastSelectedRessource = (R.drawable.black_pawn);
        }

    }

    /*
        Méthode qui dessine la pièce sélectionnée en vert, du point de vue des Noirs

     */
    private void drawPieceAsSelectedForBlack(int r, int c) {
        char piece = mTurnData.getPiece(r, c);
        if(piece == GameBoard.WHITE_ROOK){
            GameBoard.gameImageViews[7-r][7-c].setImageResource(R.drawable.white_rook_selected);
            idLastSelectedRessource = (R.drawable.white_rook);
        }else if (piece == GameBoard.WHITE_KNIGHT){
            GameBoard.gameImageViews[7-r][7-c].setImageResource(R.drawable.white_knight_selected);
            idLastSelectedRessource = (R.drawable.white_knight);
        }else if (piece == GameBoard.WHITE_BISHOP){
            GameBoard.gameImageViews[7-r][7-c].setImageResource(R.drawable.white_bishop_selected);
            idLastSelectedRessource = (R.drawable.white_bishop);
        }else if (piece == GameBoard.WHITE_QUEEN){
            GameBoard.gameImageViews[7-r][7-c].setImageResource(R.drawable.white_queen_selected);
            idLastSelectedRessource = (R.drawable.white_queen);
        }else if (piece == GameBoard.WHITE_KING){
            GameBoard.gameImageViews[7-r][7-c].setImageResource(R.drawable.white_king_selected);
            idLastSelectedRessource = (R.drawable.white_king);
        }else if (piece == GameBoard.WHITE_PAWN){
            GameBoard.gameImageViews[7-r][7-c].setImageResource(R.drawable.white_pawn_selected);
            idLastSelectedRessource = (R.drawable.white_pawn);
        }
        else if (piece == GameBoard.BLACK_ROOK){
            GameBoard.gameImageViews[7-r][7-c].setImageResource(R.drawable.black_rook_selected);
            idLastSelectedRessource = (R.drawable.black_rook);
        }else if (piece == GameBoard.BLACK_KNIGHT){
            GameBoard.gameImageViews[7-r][7-c].setImageResource(R.drawable.black_knight_selected);
            idLastSelectedRessource = (R.drawable.black_knight);
        }else if (piece == GameBoard.BLACK_BISHOP){
            GameBoard.gameImageViews[7-r][7-c].setImageResource(R.drawable.black_bishop_selected);
            idLastSelectedRessource = (R.drawable.black_bishop);
        }else if (piece == GameBoard.BLACK_QUEEN){
            GameBoard.gameImageViews[7-r][7-c].setImageResource(R.drawable.black_queen_selected);
            idLastSelectedRessource = (R.drawable.black_queen);
        }else if (piece == GameBoard.BLACK_KING){
            GameBoard.gameImageViews[7-r][7-c].setImageResource(R.drawable.black_king_selected);
            idLastSelectedRessource = (R.drawable.black_king);
        }else if (piece == GameBoard.BLACK_PAWN){
            GameBoard.gameImageViews[7-r][7-c].setImageResource(R.drawable.white_pawn_selected);
            idLastSelectedRessource = (R.drawable.black_pawn);
        }

    }

    /*
        Méthode qui redessine la pièce sélectionnée sous sa forme standard

     */
    private void redrawSelectedPieceAsStandard(boolean forWhite){
        if(rowOfPieceSelected != -1 && columnOfPieceSelected != -1 && idLastSelectedRessource != -1 && aPieceIsAlreadySelected) {
            if(forWhite){
                GameBoard.gameImageViews[rowOfPieceSelected][columnOfPieceSelected].setImageResource(idLastSelectedRessource);
                aPieceIsAlreadySelected = false;
            }
            else{
                GameBoard.gameImageViews[7-rowOfPieceSelected][7-columnOfPieceSelected].setImageResource(idLastSelectedRessource);
                aPieceIsAlreadySelected = false;
            }
        }
    }



    private void setNotation(boolean forWhite){

        String letters [] = {"a","b","c","d","e","f","g","h"};
        String numbers [] = {"1","2","3","4","5","6","7","8"};

        if(forWhite){
            for(int i = 0; i<8; i++){
                horizontalNotation[i].setText(letters[i]);
                verticalNotation[i].setText(numbers[7-i]);
            }
        }
        else{
            for(int i = 0; i<8; i++){
                horizontalNotation[i].setText(letters[7-i]);
                verticalNotation[i].setText(numbers[i]);
            }
        }
    }


}
