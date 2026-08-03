import main.java.mawsky.trafficsign.core.Pipeline;

void main() {

    // samples
    Pipeline.findSign("src/main/resources/sample/V.jpg");
    Pipeline.findSign("src/main/resources/sample/A.jpg");
    Pipeline.findSign("src/main/resources/sample/P.jpg");
    Pipeline.findSign("src/main/resources/sample/S.jpg");


    // Vorfahrtsstraße
//    Pipeline.findSign("src/main/resources/vorfahrtsstraße/vorfahrtTest1.jpg"); // success
//    Pipeline.findSign("src/main/resources/vorfahrtsstraße/vorfahrtTest2.jpg"); // success
//    Pipeline.findSign("src/main/resources/vorfahrtsstraße/vorfahrtTest3.jpg"); // success
//    Pipeline.findSign("src/main/resources/vorfahrtsstraße/vorfahrtTest4.jpg"); // success
//    Pipeline.findSign("src/main/resources/vorfahrtsstraße/vorfahrtTest5.jpg"); // success
//    Pipeline.findSign("src/main/resources/vorfahrtsstraße/vorfahrtTest6.jpg"); // success


    // achten
//    Pipeline.findSign("src/main/resources/achten/achtenTest1.jpg"); // success
//    Pipeline.findSign("src/main/resources/achten/achtenTest2.jpg"); // success
//    Pipeline.findSign("src/main/resources/achten/achtenTest3.jpg"); // success
//    Pipeline.findSign("src/main/resources/achten/achtenTest4.jpg"); // success
//    Pipeline.findSign("src/main/resources/achten/achtenTest5.jpg"); // success
//    Pipeline.findSign("src/main/resources/achten/achtenTest6.jpg"); // success
//    Pipeline.findSign("src/main/resources/achten/achtenTest7.jpg"); // success
//    Pipeline.findSign("src/main/resources/achten/achtenTest8.jpg"); // success


    // vorfahrt
//    Pipeline.findSign("src/main/resources/vorfahrt/vorfahrtTest1.jpg"); // success
//    Pipeline.findSign("src/main/resources/vorfahrt/vorfahrtTest2.jpg"); // success
//    Pipeline.findSign("src/main/resources/vorfahrt/vorfahrtTest3.jpg"); // success
//    Pipeline.findSign("src/main/resources/vorfahrt/vorfahrtTest4.jpg"); // no success, too much noise
//    Pipeline.findSign("src/main/resources/vorfahrt/vorfahrtTest5.jpg"); // success
//    Pipeline.findSign("src/main/resources/vorfahrt/vorfahrtTest6.jpg"); // success
//    Pipeline.findSign("src/main/resources/vorfahrt/vorfahrtTest7.jpg"); // success


    // stopp
//    Pipeline.findSign("src/main/resources/stopp/stoppTest1.jpg"); // false positive triangle
//    Pipeline.findSign("src/main/resources/stopp/stoppTest2.jpg"); // success
//    Pipeline.findSign("src/main/resources/stopp/stoppTest3.jpg"); // success
//    Pipeline.findSign("src/main/resources/stopp/stoppTest4.jpg"); // success
//    Pipeline.findSign("src/main/resources/stopp/stoppTest5.jpg"); // success
//    Pipeline.findSign("src/main/resources/stopp/stoppTest6.jpg"); // success

    Pipeline.shutdown();
}