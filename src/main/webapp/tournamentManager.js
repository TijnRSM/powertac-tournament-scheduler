/**
 * Created with IntelliJ IDEA.
 * User: Govert Buijs
 * Date: 8/3/12
 * Time: 4:10 PM
 * To change this template use File | Settings | File Templates.
 */

function showGamesCount() {
    var elems = saveTournament.elements;
    var gameName = elems[0].value;
    var type = elems[1].value;
    var maxBrokers = parseInt(elems[2].value);
    var maxAgents = parseInt(elems[3].value);
    var gameType1 = parseInt(elems[4].value);
    var gameType2 = parseInt(elems[6].value);
    var gameType3 = parseInt(elems[8].value);
    var multiplier1 = parseInt(elems[5].value);
    var multiplier2 = parseInt(elems[7].value);
    var multiplier3 = parseInt(elems[9].value);

    setText("totalGames", "");
    setText("total1", "");
    setText("total2", "");
    setText("total3", "");

    if (type == 'SINGLE_GAME') {
        return;
    }

    var gameDuration = (gameName.toLowerCase().indexOf("test") > -1) ? 0.33 : 2;
    var totalGames1 = 0;
    var totalGames2 = 0;
    var totalGames3 = 0;
    var totalTime1 = 0;
    var totalTime2 = 0;
    var totalTime3 = 0;
    if ((maxBrokers > 0) && (gameType1 > 0) && (multiplier1 > 0)) {
        if (gameType1 > maxBrokers) {
            elems[4].value = maxBrokers;
            gameType1 = maxBrokers;
        }
        totalGames1 = calculateGames(maxBrokers, gameType1, multiplier1);
        totalTime1 = gameDuration * (totalGames1 * gameType1) / (maxBrokers * maxAgents);
        setText("total1", "Games for this type : " + totalGames1);
    }
    if ((maxBrokers > 0) && (gameType2 > 0) && (multiplier2 > 0)) {
        if (gameType2 > maxBrokers) {
            elems[6].value = maxBrokers;
            gameType2 = maxBrokers;
        }
        totalGames2 = calculateGames(maxBrokers, gameType2, multiplier2);
        totalTime2 = gameDuration * (totalGames2 * gameType2) / (maxBrokers * maxAgents);
        setText("total2", "Games for this type : " + totalGames2);
    }
    if ((maxBrokers > 0) && (gameType3 > 0) && (multiplier3 > 0)) {
        if (gameType3 > maxBrokers) {
            elems[8].value = maxBrokers;
            gameType3 = maxBrokers;
        }
        totalGames3 = calculateGames(maxBrokers, gameType3, multiplier3);
        totalTime3 = gameDuration * (totalGames3 * gameType3) / (maxBrokers * maxAgents);
        setText("total3", "Games for this type : " + totalGames3);
    }

    var total = totalGames1 + totalGames2 + totalGames3;
    var duration = totalTime1 + totalTime2 + totalTime3;
    if (total > 0) {
        setText("totalGames", "Total games to be created : " + total);
        setText("totalTime", "Estimated duration : " + parseInt(1.1 * duration) +" hours");
    }
}

function setText(fieldId, newText) {
    var el = document.getElementById(fieldId);
    while(el.childNodes.length >= 1) {
        el.removeChild(el.firstChild);
    }
    el.appendChild(el.ownerDocument.createTextNode(newText));
}

function calculateGames(players, gametype, multiplier) {
    if (players == gametype) {
        return multiplier;
    }
    if (gametype == 1) {
        return players * multiplier;
    }
    return calculateGames(players-1, gametype, multiplier) + calculateGames(players-1, gametype-1, multiplier);
}

function typeSelected() {
    var elems = saveTournament.elements;
    var type = elems[1].value;
    var table = document.getElementById("saveTournament:tournamentTable");

    if (type == 'MULTI_GAME') {
        table.rows[3].style.display = "";
        table.rows[4].style.display = "";
        table.rows[5].style.display = "";
        table.rows[6].style.display = "";
        table.rows[7].style.display = "";
        table.rows[8].style.display = "";
        table.rows[9].style.display = "";
    } else if (type == 'SINGLE_GAME') {
        table.rows[3].style.display = "none";
        table.rows[4].style.display = "none";
        table.rows[5].style.display = "none";
        table.rows[6].style.display = "none";
        table.rows[7].style.display = "none";
        table.rows[8].style.display = "none";
        table.rows[9].style.display = "none";
    }

    showGamesCount();
}

window.onload = function ()
{
    typeSelected();
};