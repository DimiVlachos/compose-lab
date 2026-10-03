import CoreTransferable
import MoodboardShared
import SwiftUI
import UniformTypeIdentifiers

struct PhotoRoute: Hashable {
    let id: String
}

struct BoardRoute: Hashable {
    let id: String
}

extension Photo: @retroactive Identifiable {}
extension Board: @retroactive Identifiable {}
extension BoardSummary: @retroactive Identifiable {}

/// A photo for the system share sheet; the bytes load only once the person picks a target.
struct SharedPhoto: Transferable {
    let path: String

    static var transferRepresentation: some TransferRepresentation {
        DataRepresentation(exportedContentType: .jpeg) { photo in
            try await ImageDataKt.imageData(path: photo.path) ?? Data()
        }
    }
}
