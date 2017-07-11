package game.ai;

import card.Ability;
import card.Card;
import card.EnergyCard;
import card.PokemonCard;
import game.GameBoard;
import game.Player;
import game.TargetSelector;
import java.util.List;
import java.util.Random;
import parser.abilities.AbilityPart;
import parser.abilities.AbilityPartDam;
import parser.cards.EnergyCost;
import parser.commons.TargetProperty;
import ui.selections.RewardSelector;
import ui.selections.TargetSelectorUI;

/**
 * Created by frede on 2017-07-08.
 */
public class IntelligentPlayer extends Player {

    public IntelligentPlayer(List<Card> playerDeck) {
        super(playerDeck);
    }
    
    public void doTurn(GameBoard gameBoard){
        if(activePokemon == null){
            PokemonCard optimalActive = findOptimalPokemon();
            if(optimalActive != null) {
                gameBoard.onHandCardClicked(this, optimalActive);
                gameBoard.onActiveCardClicked(this, null);
            }
        }else{
            PokemonCard optimalBench = findOptimalPokemon();
            if(optimalBench != null){
                gameBoard.onHandCardClicked(this, optimalBench);
                gameBoard.onBenchCardClicked(this, null);
            }
        }
        
        playEnergyCard(gameBoard);
        tryAttack(gameBoard);
    }
    
    private PokemonCard findOptimalPokemon(){
        int bestScore = -1;
        int energyScore = -1;
        
        PokemonCard bestCard = null;
        
        EnergyCost energyInHand = new EnergyCost();
        for(Card card : hand){
            if(card instanceof EnergyCard){
                EnergyCard energyCard = (EnergyCard)card;
                switch(energyCard.getEnergyType()){
                    case FIGHT:
                        energyInHand.fight++;
                        break;
                    case WATER:
                        energyInHand.water++;
                        break;
                    case LIGHTNING:
                        energyInHand.lightning++;
                        break;
                    case PSYCHIC:
                        energyInHand.psychic++;
                        break;
                }
            }
        }
        
        for(Card card : hand){
            if(card instanceof PokemonCard){
                int score = 0;
                
                PokemonCard pokemonCard = (PokemonCard)card;
                EnergyCost energyUsed = new EnergyCost();
                for(Ability ability : pokemonCard.getAbilities()){
                    energyUsed.add(ability.getEnergyCost());
                    
                    
                    for(AbilityPart part: ability.getTemplate().parts){
                        if(part instanceof AbilityPartDam){
                            AbilityPartDam abilityPartDam = (AbilityPartDam)part;
                            score += abilityPartDam .getAmmount().evaluateAsExpression();
                        }
                    }
                }
                int matchingEnergy = 0;
                int[] asArray = energyInHand.getAsArray();
                for (int i = 0; i < asArray.length; i++) {
                    int handEnergy = asArray[i];
                    int pokemonEnergy = energyUsed.getAsArray()[i];
                    
                    if(pokemonEnergy > 0 && handEnergy > 0){
                        matchingEnergy++;
                    }
                }
                
                score *= ((double)matchingEnergy)/5+0.2;
                
                if(score > bestScore){
                    bestScore = score;
                    bestCard = pokemonCard;
                }
            }
        }
        return bestCard;
    }
    
    private void playEnergyCard(GameBoard gameBoard){
        if(activePokemon != null){
            EnergyCost neededEnergy = new EnergyCost();
            EnergyCost currentEnergy = activePokemon.getEnergyAttached();
            activePokemon.getAbilities().forEach(ability -> {
                int neededColorless = ability.getEnergyCost().colorless - currentEnergy.colorless;
                if(neededColorless > neededEnergy.colorless){
                    neededEnergy.colorless = neededColorless;
                }
                
                int neededWater = ability.getEnergyCost().water - currentEnergy.water;
                if(neededWater > neededEnergy.water){
                    neededEnergy.water = neededWater;
                }
                
                int neededLightning = ability.getEnergyCost().lightning - currentEnergy.lightning;
                if(neededLightning > neededEnergy.lightning){
                    neededEnergy.lightning = neededLightning;
                }
                
                int neededPsychic = ability.getEnergyCost().psychic - currentEnergy.psychic;
                if(neededPsychic > neededEnergy.psychic){
                    neededEnergy.psychic = neededPsychic;
                }
                
                int neededFight = ability.getEnergyCost().fight- currentEnergy.fight;
                if(neededFight > neededEnergy.fight){
                    neededEnergy.fight = neededFight;
                }
                
            });
            
            EnergyCard energyToPlay = null;
            
            search:
            for(Card card : hand){
                if(card instanceof EnergyCard){
                    EnergyCard energyCard = (EnergyCard)card;
                    switch(energyCard.getEnergyType()){
                        case FIGHT:
                            if(neededEnergy.fight <= 0 && neededEnergy.colorless <=0){
                                break;
                            }
                            
                        case WATER:
                            if(neededEnergy.water <= 0 && neededEnergy.colorless <=0){
                                break;
                            }
                        case PSYCHIC:
                            if(neededEnergy.psychic <= 0 && neededEnergy.colorless <=0){
                                break;
                            }
                        case LIGHTNING:
                            if(neededEnergy.lightning <= 0 && neededEnergy.colorless <=0){
                                break;
                            }

                        default:
                            energyToPlay = energyCard;
                            break search;
                            
                    }
                }
            }
            
            if(energyToPlay != null){
                gameBoard.onHandCardClicked(this, energyToPlay);
                gameBoard.onActiveCardClicked(this, activePokemon);
            }
            
        }
    }
    
    private void tryAttack(GameBoard gameBoard){
        if(activePokemon != null){
            for(Ability ability : activePokemon.getAbilities()){
                if(ability.getTemplate().appliesDamage()){
                    gameBoard.onActiveAbilityClicked(this, activePokemon, ability);
                    return;
                }
            }
        }
    }
    public void choseRewardCard() {
        if(prizes.size() > 0) {
            int prizeId = new Random(System.currentTimeMillis()).nextInt(prizes.size());
            hand.add(prizes.remove(prizeId));
        }
        
    }
    public TargetSelector createTargetSelector(){
        return new AiTargetSelector();
    }
    
    public class AiTargetSelector extends TargetSelector{
        
        public Card getCard(GameBoard gameBoard, Player callingPlayer, TargetProperty targetProperty){
            return getAiCard(gameBoard, callingPlayer, targetProperty);
        }

        public Card getChoiceCard(GameBoard gameBoard, Player callingPlayer, TargetProperty targetProperty){
            return getAiCard(gameBoard, callingPlayer, targetProperty);
        }
        
        private Card getAiCard(GameBoard gameBoard, Player callingPlayer, TargetProperty targetProperty){
            switch(targetProperty.target.value){
                case "choice":{
                    switch(targetProperty.modifier.value){
                        case "opponent-bench":{
                            Player otherPlayer = gameBoard.getOtherPlayer(callingPlayer);
                            if(otherPlayer.getBench().size() > 0) {
                                int cardToSelect = new Random(System.currentTimeMillis()).nextInt(
                                    otherPlayer.getBench().size());
                                return otherPlayer.getBench().get(cardToSelect);
                            }
                        }
                    }
                }

                case "opponent-active":{
                    return getOpponentActive(gameBoard, callingPlayer);
                }

                default: {
                    return null;
                }
            }
        }
    }
}
