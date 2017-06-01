package game;

import card.Card;
import card.Ability;
import card.EnergyCard;
import card.PokemonCard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.*;
import ui.GameOutcomePopup;

/**
 * Created by frede on 2017-05-15.
 */
public class GameBoard {
  
  final static Logger logger = LogManager.getLogger(GameBoard.class.getName());
  
  private Player[] players;

  private int currentTurn = 0;

  private int turnNum = 0;
  
  private Card selectedCard = null;
  private CardLocation selectedCardLocation = null;
  Random rand = new Random();
  public GameBoard(Player p1, Player p2) {
      players = new Player[2];
      players[0] = p1;
      players[1] = p2;
  }

  public void setSelectedCard(Card card, CardLocation location){
    if(selectedCard != null) {
      selectedCard.setSelected(false);
    }

    selectedCard = card;
    if(selectedCard != null){
      selectedCard.setSelected(true);
    }
    selectedCardLocation = location;
  }

  public void onHandCardClicked(Player player, Card card){
    int playerNum = (player == players[0])?1:2;
    logger.debug("Player"+playerNum+" has clicked a card in it's hand");

    if(card != null && (playerNum-1) == currentTurn) {
      setSelectedCard(card, CardLocation.HAND);
    }

  }

  public void onBenchCardClicked(Player player, Card card){
    int playerNum = (player == players[0])?1:2;
    logger.debug("Player"+playerNum+" has clicked a card in it's bench");
    if(selectedCard != null) {
      if (card != null && player == getCurrentTurnPlayer() 
          && selectedCard instanceof EnergyCard) {
        if (card instanceof PokemonCard) {
          PokemonCard pokemonCard = (PokemonCard) card;
          EnergyCard energyCard = (EnergyCard) selectedCard;
          pokemonCard.getEnergyAttached().addEnergy(energyCard.getEnergyType().toString(), 1);
          player.getHand().remove(energyCard);
          selectedCard = null;
        }
      }

      //Player is trying to place pokemon card on bench
      if (selectedCardLocation == CardLocation.HAND
          && selectedCard instanceof PokemonCard && player == getCurrentTurnPlayer()) {
        PokemonCard selectedPokemon = (PokemonCard) selectedCard;

        if (selectedPokemon.getPokemonStage() == "stage-one") {
          if (selectedPokemon.getEvolvesFrom() == card.getCardName()) {
            player.setActivePokemon(selectedPokemon);
            player.getHand().remove(selectedPokemon);
          }
        } else {
          //remove selected card from player's hand and put it on the player's bench
          if (players[0].getHand().remove(selectedCard)) {
            players[0].getBench().add(selectedCard);
            setSelectedCard(null, null);
          }
        }
        return;
      }
    }else{
        setSelectedCard(card, CardLocation.BENCH);
    }

  }

  public void onActiveCardClicked(Player player, Card card){
    int playerNum = (player == players[0])?1:2;
    logger.debug("Player"+playerNum+" has clicked the active pokemon");
    
    PokemonCard pokemonCard = (PokemonCard)card;

    if(card != null && player == getCurrentTurnPlayer() && selectedCard != null && selectedCard instanceof EnergyCard){
      EnergyCard energyCard = (EnergyCard)selectedCard;
      pokemonCard.getEnergyAttached().addEnergy(energyCard.getEnergyType().toString(), 1);
      player.getHand().remove(energyCard);
      selectedCard = null;
    }
    
    if(player.getActivePokemon() != null){
        if(selectedCard instanceof PokemonCard){
            PokemonCard selectedPokemon = (PokemonCard)selectedCard;
            if(selectedPokemon.getPokemonStage() == "stage-one"){
                if(selectedPokemon.getEvolvesFrom() == pokemonCard.getCardName()){
                    player.setActivePokemon(selectedPokemon);
                    player.getHand().remove(selectedPokemon);
                }
            }
        }
    }else{
      if(selectedCard != null && selectedCard instanceof PokemonCard && player == getCurrentTurnPlayer()){
        PokemonCard selectedPokemon = (PokemonCard)selectedCard;
        if(selectedPokemon.getPokemonStage().equalsIgnoreCase("basic")) {
          //remove selected card from player's hand and put it as active
          if (removeSelectedCard()) {
            player.setActivePokemon(selectedCard);
            setSelectedCard(null, null);
          }
        }
      }
    }
    
  }
  
  public boolean removeSelectedCard(){
      switch(selectedCardLocation){
        case HAND:
          return getCurrentTurnPlayer().getHand().remove(selectedCard);
        case BENCH:
          return getCurrentTurnPlayer().getBench().remove(selectedCard);
      }
      return false;
  }
  
 public void onActiveAbilityClicked(Player player, Card card, Ability ability){
    int playerNum = (player == players[0])?1:2;
    logger.debug("Player " + playerNum + " has clicked "+ability.getTemplate().name + " on "+card.getCardName());
    if(player == getCurrentTurnPlayer()) {
      ability.getTemplate().use(this, player);
      checkPokemons();
    }
 }
 
 public void checkPokemons(){
   for (Player player : players) {
        if(player.getActivePokemon() != null){
            PokemonCard pokemonCard = (PokemonCard)player.getActivePokemon();
            if(pokemonCard.getDamage() >= pokemonCard.getHp()){
                player.setActivePokemon(null);
                onCardDead(player);
            }
        }
        player.getBench().forEach((card -> {
            if(card instanceof PokemonCard) {
              PokemonCard pokemonCard = (PokemonCard) card;
              if (pokemonCard.getDamage() >= pokemonCard.getHp()) {
                player.getBench().remove(card);
                onCardDead(player);
              }
            }
        }));
       player.getHand().forEach((card -> {
         if(card instanceof PokemonCard) {
           PokemonCard pokemonCard = (PokemonCard) card;
           if (pokemonCard.getDamage() >= pokemonCard.getHp()) {
             player.getHand().remove(card);
             onCardDead(player);
           }
         }
       }));
   }
 }
 
 public void onCardDead(Player owner){
      Player otherPlayer = getOtherPlayer(owner);
      otherPlayer.choseRewardCard();
      int playerNum = (otherPlayer == players[0])?1:2;
      if(otherPlayer.getPrizes().size() == 0){
        
        GameOutcomePopup.display("player" + playerNum, true);
      }
      
      if(turnNum > 2){
          if(owner.getActivePokemon() == null && owner.getBench().isEmpty()){
            GameOutcomePopup.display("player" + playerNum, true);
          }
      }
 }

  public void onEndTurnButtonClicked(){
    checkWinLoose();
    nextTurn();
    
    //TODO process AI turn

    //finish AI turn
   // nextTurn();
  }

  public void nextTurn(){
    //This will cycle between 0 and 1
    currentTurn = (currentTurn + 1)%2;
    turnNum++;
    Player currentPlayer = getCurrentTurnPlayer();
    
    if(currentPlayer.getDeck().isEmpty()){
      Player otherPlayer = getOtherPlayer(currentPlayer);
      int playerNum = (otherPlayer == players[0])?1:2;
      GameOutcomePopup.display("player" + playerNum, true);
    }
    
    //add card to players hand
    currentPlayer.putCardInHand();

    if(currentTurn == 1)
        aiTurn();

  }
  public void aiTurn(){

    int cardTOAddToBench = rand.nextInt(5);
      if(players[1].activePokemon == null)
          players[1].chooseActivePokemon();
      for(int i = 0 ; i<(5- cardTOAddToBench); i++)
      {
          players[1].putCardOnBench();
      }

      //players[1].putCardOnBench();
      //players[1].activePokemon  this is suppose to attack
      nextTurn();
  }
  public void checkWinLoose(){

      List<Card>  pCards  = new ArrayList() ;
      if(getCurrentTurnPlayer().prizes.size() ==0 || players[((currentTurn + 1)%2)].deck.size()==0)
      {
          for(Card c : players[((currentTurn + 1)%2)].getBench())
              if( c instanceof  PokemonCard)//  .getType().equals("POKEMON"))
                pCards.add(c);
          if(pCards.size() == 0)
          {
              win();
          }


      }
  }
  public void win(){
      System.out.println("player "+ currentTurn + "has won the game");
      System.exit(0);
  }
  public Player[] getPlayers(){
    return players;
  }
  
  public Player getPlayer1(){
    return players[0];
  }
  
  public Player getPlayer2(){
    return players[1];
  }

  public Player getCurrentTurnPlayer(){
    return players[currentTurn];
  }

  public Player getOppositeTurnPlayer(){
    return players[(currentTurn+1)%2];
  }
  
  public Player getOtherPlayer(Player player){
    if(player == players[0]) {
      return players[1];
    }
    
    return players[0];
  }
  
  public void onRetreatButtonClicked(Player player){
      
  }
}
