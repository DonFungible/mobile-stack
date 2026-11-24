import AppCore
import Foundation
import IntrospectionKit

// The headless host: the same core the app runs, constructed with fixtures instead of platform services.
//
//   mobilestack-cli --fixtures-dir <dir> [--fixtures Name,Name] [--app-version 1.2.3]
//
// Then one command per line on stdin, one answer per line on stdout: `ls`, `do Action [json]`,
// or the JSON form the test SDK speaks. The JSON form adds `boot`, which replaces the running core
// with a fresh one on new fixtures, so one warm process can serve a whole test run.

func argument(_ name: String) -> String? {
    let arguments = CommandLine.arguments
    return arguments.firstIndex(of: name).flatMap { arguments.indices.contains($0 + 1) ? arguments[$0 + 1] : nil }
}

func fail(_ message: String) -> Never {
    FileHandle.standardError.write(Data("mobilestack-cli: \(message)\n".utf8))
    exit(2)
}

guard let directory = argument("--fixtures-dir") else {
    fail("--fixtures-dir is required")
}

let host = Host(fixturesDirectory: URL(fileURLWithPath: directory))
if let names = argument("--fixtures") {
    do {
        try host.boot(fixtures: names.split(separator: ",").map(String.init), appVersion: argument("--app-version"))
    } catch {
        fail("cannot load fixtures \(names): \(error)")
    }
}

// Reading stdin asynchronously leaves the main actor free between commands, so service results land.
for try await line in FileHandle.standardInput.bytes.lines {
    FileHandle.standardOutput.write(Data((host.handle(line: line) + "\n").utf8))
}
