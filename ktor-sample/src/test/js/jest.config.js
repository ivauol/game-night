module.exports = {
    testEnvironment: "jsdom",
    reporters: [
        "default",
        ["jest-junit", {
            outputDirectory: process.env.test_output,
            outputName: "TEST-JsTest.xml"
        }]
    ]
};