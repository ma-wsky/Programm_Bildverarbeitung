import classes.Pipeline.Pipeline;

void main() {

//    //samples
    //Pipeline.findSign("pics/sample/V.jpg");
    //Pipeline.findSign("pics/sample/A.jpg");
    //Pipeline.findSign("pics/sample/P.jpg");
    //Pipeline.findSign("pics/sample/S.jpg");

    //TODO: a lot of false positive triangle and rectangle shapes are found -> size constraint for when shapes are valid

    // vorfahrtsstrasse
    Pipeline.findSign("pics/vorfahrt/vorfahrtTest1.jpg"); // success
//    Pipeline.findSign("pics/vorfahrt/vorfahrtTest2.jpg"); // no rect found -> side ratio tolerance increased to .2 -> success
//    Pipeline.findSign("pics/vorfahrt/vorfahrtTest3.jpg"); // success
//    Pipeline.findSign("pics/vorfahrt/vorfahrtTest4.jpg"); // tree consumes edges -> less closing -> success
//    Pipeline.findSign("pics/vorfahrt/vorfahrtTest5.jpg"); // too many garbage lines get found -> need better checking for validity (moving window/sectioned search)
//    Pipeline.findSign("pics/vorfahrt/vorfahrtTest6.jpg"); // needed to lower ratioYellowWhite lower bound because center is obstructed -> success

    //TODO: too many garbage lines -> better check for validity of lines

    // achten
//    Pipeline.findSign("pics/achten/achtenTest1.jpg"); // entropy dismissed, white value lowered -> success
//    Pipeline.findSign("pics/achten/achtenTest2.jpg"); // edges get lost from Sobel to equidensity
//    Pipeline.findSign("pics/achten/achtenTest3.jpg"); // no triangle found -> higher angle tolerance and side ratio tolerance, higher ratioRedWhite upper bound -> success
//    Pipeline.findSign("pics/achten/achtenTest4.jpg"); // noise produces too much lines -> try sectioned checks (moving window)
//    Pipeline.findSign("pics/achten/achtenTest5.jpg"); // didn't find crooked top line
//    Pipeline.findSign("pics/achten/achtenTest6.jpg"); // lots of noise from roof. couldn't find left edge

    //TODO: too much noise -> better remedy needed (sectioned search / moving window)

    // vorfahrt
//    Pipeline.findSign("pics/pfeil/vorfahrtTest1.jpg"); // hab to tweak color detection again -> success
//    Pipeline.findSign("pics/pfeil/vorfahrtTest2.jpg"); // success
//    Pipeline.findSign("pics/pfeil/vorfahrtTest3.jpg"); // success
//    Pipeline.findSign("pics/pfeil/vorfahrtTest4.jpg"); // couldn't find triangle due to tree noise

    //TODO: noise

    // stopp
//    Pipeline.findSign("pics/stopp/stoppTest1.jpg"); // not all lines of octagon found -> noise
//    Pipeline.findSign("pics/stopp/stoppTest2.jpg"); // other edges are more defined
//    Pipeline.findSign("pics/stopp/stoppTest3.jpg"); // not all edges found -> too many lines because of text in middle
//    Pipeline.findSign("pics/stopp/stoppTest4.jpg"); // not all edges found -> same reason
    Pipeline.findSign("pics/stopp/stoppTest5.jpg"); // increased upper bound of ratioRedWhite -> success

    //TODO: too many garbage lines and noise, focus on other signs

}