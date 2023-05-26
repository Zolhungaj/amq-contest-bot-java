package tech.zolhungaj.amqcontestbot.bonus;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.zolhungaj.amqcontestbot.chat.ChatCommands;
import tech.zolhungaj.amqcontestbot.chat.ChatController;
import tech.zolhungaj.amqcontestbot.repository.PlayerService;

@Component
@RequiredArgsConstructor
public class Valour {
    private final ChatCommands chatCommands;
    private final ChatController chatController;
    private final PlayerService playerService;

    @PostConstruct
    private void init(){
        chatCommands.register((sender, arguments) -> {
            if(arguments.size() == 1){
                double value = Double.parseDouble(arguments.get(0));
                String result = romanNumerals(value);
                chatController.send("bonus.roman", result);
            }else{
                throw new IllegalArgumentException();
            }
        }, "roman");
    }

    private String romanNumerals(double doubleValue){
        StringBuilder output = new StringBuilder();
        if(doubleValue < 0){
            //handle negative numbers
            output.append("÷");
        }
        doubleValue = Math.abs(doubleValue);
        int remainder = (int) Math.round((doubleValue - Math.floor(doubleValue))*12); //should be a number in the range 0-12
        int value = (int) Math.floor(doubleValue);
        if(remainder == 12){
            value++;
            remainder = 0;
        }
        if(value > 3999){
            //not super keen on supporting non-standard roman numerals, so I just give the non-standard 100k here.
            return output.append("ↈ").toString();
        }
        if(value == 0 && remainder == 0){
            return output.append("☆").toString();
        }
        final String thousand = "Ⅿ";
        final String fivehundred = "Ⅾ";
        final String hundred = "Ⅽ";
        final String fifty = "Ⅼ";
        final String[] onetotwelve = {"","Ⅰ","Ⅱ","Ⅲ","Ⅳ","Ⅴ","Ⅵ","Ⅶ","Ⅷ","Ⅸ","Ⅹ","Ⅺ","Ⅻ"};
        //handle thousands
        output.append(thousand.repeat(value/1000));
        value %= 1000;
        //handle 500-999
        if(value == 999){
            output.append(onetotwelve[1]).append(thousand);
            value -= 999;
        }else if(value >= 990){
            output.append(onetotwelve[10]).append(thousand);
            value -= 990;
        }else if(value >= 900){
            output.append(hundred).append(thousand);
            value -= 900;
        }else if(value >= 500){
            output.append(fivehundred);
            value -= 500;
        }
        //handle 400-499
        if(value == 499){
            output.append(onetotwelve[1]).append(fivehundred);
            value -= 499;
        }else if(value >= 490){
            output.append(onetotwelve[10]).append(fivehundred);
            value -= 490;
        }else if(value >= 400){
            output.append(hundred).append(fivehundred);
            value -= 400;
        }
        //handle 100-300
        output.append(hundred.repeat(value/100));
        value %= 100;
        //handle 40-99
        if(value == 99){
            output.append(onetotwelve[1]).append(hundred);
            value -= 99;
        }else if(value >= 90){
            output.append(onetotwelve[10]).append(hundred);
            value -= 90;
        }else if(value >= 50){
            output.append(fifty);
            value -= 50;
        }else if(value == 49){
            output.append(onetotwelve[1]).append(fifty);
            return output.toString();
        }else if(value >= 40){
            output.append(onetotwelve[10]).append(fifty);
            value -= 40;
        }
        //handle 1-39
        while(value > 12){
            //append 10 until we reach 12 (since 12 is the largest single-character value we cannot use String.repeat)
            output.append(onetotwelve[10]);
            value -= 10;
        }
        //append the final numeral
        output.append(onetotwelve[value]);

        //handle the decimals
        if(remainder > 0){
            //fractions in roman numerals are given as number of dots/12, S represents 6 dots
            String[] representations = {"", "·", ":", "∴", "∷", "⁙", "S"};
            if(remainder >= 6){
                output.append(representations[6]);
                remainder -= 6;
            }
            output.append(representations[remainder]);
        }
        return output.toString();
    }
}
