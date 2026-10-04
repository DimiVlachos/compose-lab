import MoodboardShared
import SwiftUI

extension View {
    /// A system alert with one text field: new board and rename board.
    func nameAlert(
        _ title: String,
        isPresented: Binding<Bool>,
        name: Binding<String>,
        confirm: String,
        onConfirm: @escaping (String) -> Void
    ) -> some View {
        alert(title, isPresented: isPresented) {
            TextField("Name", text: name)
            Button(confirm) {
                let trimmed = name.wrappedValue.trimmingCharacters(in: .whitespaces)
                if !trimmed.isEmpty { onConfirm(trimmed) }
            }
            Button("Cancel", role: .cancel) {}
        }
    }

    /// A text-field alert tied to the item it acts on, which it receives in its confirm action.
    func nameAlert<Item>(
        _ title: String,
        item: Binding<Item?>,
        name: Binding<String>,
        confirm: String,
        onConfirm: @escaping (Item, String) -> Void
    ) -> some View {
        alert(
            title,
            isPresented: Binding(get: { item.wrappedValue != nil }, set: { if !$0 { item.wrappedValue = nil } }),
            presenting: item.wrappedValue
        ) { target in
            TextField("Name", text: name)
            Button(confirm) {
                let trimmed = name.wrappedValue.trimmingCharacters(in: .whitespaces)
                if !trimmed.isEmpty { onConfirm(target, trimmed) }
            }
            Button("Cancel", role: .cancel) {}
        }
    }

    /// The system delete confirmation for a photo.
    func deletePhotoAlert(_ photo: Binding<Photo?>, onDelete: @escaping (Photo) -> Void) -> some View {
        alert(
            "Delete photo?",
            isPresented: Binding(get: { photo.wrappedValue != nil }, set: { if !$0 { photo.wrappedValue = nil } }),
            presenting: photo.wrappedValue
        ) { target in
            Button("Delete", role: .destructive) { onDelete(target) }
            Button("Cancel", role: .cancel) {}
        } message: { target in
            Text("“\(target.title)” will be removed from the gallery and every board.")
        }
    }
}
