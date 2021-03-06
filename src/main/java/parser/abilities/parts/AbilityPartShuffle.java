package parser.abilities.parts;

import card.Card;
import game.GameBoard;
import game.Player;
import game.SpecialAbility;
import parser.abilities.properties.TargetProperty;

public class AbilityPartShuffle extends AbilityPart{

    private TargetProperty target;

    public AbilityPartShuffle(TargetProperty target) {
        super("shuffle");
        this.target = target;
        
        properties.add(target);
    }

    @Override
    public boolean use(GameBoard targetBoard, Player owner, Card callingCard) {
        switch(target.target.value){
            case "your":{
                SpecialAbility.shuffleDeck(targetBoard.getCurrentTurnPlayer().getDeck());
            }break;
            
            case "opponent":{
                SpecialAbility.shuffleDeck(targetBoard.getOtherPlayer(owner).getDeck());
            }break;
        }
        return false;
    }

    @Override
    public String getDescriptionString() {
        return "Suffles " + target;
    }
}
