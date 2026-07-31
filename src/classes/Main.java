import classes.Pipeline.Pipeline;

void main() {

//    //samples
    Pipeline.findSign("pics/sample/V.jpg");
    Pipeline.findSign("pics/sample/A.jpg");
    Pipeline.findSign("pics/sample/P.jpg");
    Pipeline.findSign("pics/sample/S.jpg");

    // TODO: sort things out with (- diagonal) everywhere
    // TODO: sort out helpers from formchecker into formcheckhelper
    // TODO: tri color seems to be to lenient some times (detected while reworking octagon). no false case since working octagon rework though

    // Vorfahrtsstraße
//    Pipeline.findSign("pics/vorfahrt/vorfahrtTest1.jpg"); // success
//    Pipeline.findSign("pics/vorfahrt/vorfahrtTest2.jpg"); // no rect found -> side ratio tolerance increased to .2 -> success
//    Pipeline.findSign("pics/vorfahrt/vorfahrtTest3.jpg"); // success
//    Pipeline.findSign("pics/vorfahrt/vorfahrtTest4.jpg"); // tree consumes edges -> less closing -> success
//    Pipeline.findSign("pics/vorfahrt/vorfahrtTest5.jpg"); // too many garbage lines get found -> need better checking for validity (moving window/sectioned search) -> success
//    Pipeline.findSign("pics/vorfahrt/vorfahrtTest6.jpg"); // needed to lower ratioYellowWhite lower bound because center is obstructed -> success


    // achten
//    Pipeline.findSign("pics/achten/achtenTest1.jpg"); // entropy dismissed, white value lowered -> success, false positives with moving window -> windowSize 300, step 50 -> success
//    Pipeline.findSign("pics/achten/achtenTest2.jpg"); // edges get lost from Sobel to equidensity -> histogram equalization -> success
//    Pipeline.findSign("pics/achten/achtenTest3.jpg"); // no triangle found -> higher angle tolerance and side ratio tolerance, higher ratioRedWhite upper bound, moving window angles to steep -> success -> no success
//    Pipeline.findSign("pics/achten/achtenTest4.jpg"); // noise produces too much lines -> try sectioned checks (moving window), moving window -> lines not found -> centerColor check is faulty -> success
//    Pipeline.findSign("pics/achten/achtenTest5.jpg"); // didn't find crooked top line -> centerColor check is faulty -> success
//    Pipeline.findSign("pics/achten/achtenTest6.jpg"); // lots of noise from roof. couldn't find left edge -> centerColor check is faulty -> success
//    Pipeline.findSign("pics/achten/bild.jpg"); // centerColor check is faulty -> success


    // vorfahrt
//    Pipeline.findSign("pics/pfeil/vorfahrtTest1.jpg"); // hab to tweak color detection again -> success
//    Pipeline.findSign("pics/pfeil/vorfahrtTest2.jpg"); // moving window no success -> histogram equal -> success
//    Pipeline.findSign("pics/pfeil/vorfahrtTest3.jpg"); // success -> false positive red wall -> tri edge check -> interesting false positives -> success
//    Pipeline.findSign("pics/pfeil/vorfahrtTest4.jpg"); // couldn't find triangle due to tree noise and colour -> noise costs a lot of performance
//    Pipeline.findSign("pics/pfeil/vf_03.jpg"); // false triangle -> need tri-edge color check -> success -> DIFFERENT RESULT WHEN RUNNING MULTIPLE TIMES
//    Pipeline.findSign("pics/pfeil/vf_04.jpg"); // success -> false positive is interesting -> still false positives with tri edge check ????
//    Pipeline.findSign("pics/pfeil/vf_05.jpg"); // false positive check -> no false positives (sign not fully included in window)


    // stopp
//    Pipeline.findSign("pics/stopp/stoppTest1.jpg"); // not all lines of octagon found -> noise
//    Pipeline.findSign("pics/stopp/stoppTest2.jpg"); // other edges are more defined -> octagon construction algo 7 -> success
//    Pipeline.findSign("pics/stopp/stoppTest3.jpg"); // not all edges found -> too many lines because of text in middle -> octagon construction algo 6 -> higher tolerance of sidelength ratio -> success
//    Pipeline.findSign("pics/stopp/stoppTest4.jpg"); // not all edges found -> same reason -> octagon construction algo 6 -> higher tolerance of sidelength ratio -> success
//    Pipeline.findSign("pics/stopp/stoppTest5.jpg"); // increased upper bound of ratioRedWhite -> success
//    Pipeline.findSign("pics/stopp/stop_6.jpg"); // noise due to letters and bird -> octagon construction algo 6 -> higher tolerance of sidelength ratio -> success

    Pipeline.shutdown();
}