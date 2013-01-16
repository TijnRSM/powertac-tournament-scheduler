/**
 * Created with IntelliJ IDEA.
 * User: govert
 * Date: 1/16/13
 * Time: 3:49 PM
 */

function resizeTables() {
    $("[id$=dataresultsSingle]").dataTable({
        "bFilter": false,
        "bInfo": false,
        "bPaginate": false
    });

    $("[id$=dataresultsMulti]").dataTable({
        "bFilter": false,
        "bInfo": false,
        "bPaginate": false,
        "aoColumnDefs": [
            { "sType": "natural", "aTargets": [0] }
        ]
    });

    $('[id$=games]').dataTable({
        "bFilter": false,
        "bInfo": false,
        "sScrollY": Math.min(500, $("[id$=games]").height()) + "px",
        "bPaginate": false,
        "aoColumnDefs": [
            { 'bSortable': false, 'aTargets': [3, 4] },
            { "sType": "natural", "aTargets": [0, 1, 2] }
        ]
    });
}

$(document).ready(function () {
    resizeTables();
});