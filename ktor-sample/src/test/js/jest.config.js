module.exports = {
    testEnvironment: "jsdom",
    reporters: [
        "default",
        ["jest-junit", {
            outputDirectory: "../../../build/test-results/jstest",
            outputName: "TEST-JsTest.xml"
        }]
    ]
};