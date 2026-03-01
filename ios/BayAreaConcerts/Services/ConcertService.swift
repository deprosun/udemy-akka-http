import Foundation

@MainActor
class ConcertService: ObservableObject {
    @Published var concerts:  [Concert] = []
    @Published var isLoading: Bool      = false
    @Published var error:     String?

    // In the simulator, localhost resolves to the Mac running the backend.
    // On a real device, replace with your Mac's local IP (e.g. http://192.168.1.x:8080).
    private let endpoint = URL(string: "http://localhost:8080/api/concerts")!

    func fetchConcerts() {
        isLoading = true
        error     = nil

        URLSession.shared.dataTask(with: endpoint) { [weak self] data, _, taskError in
            DispatchQueue.main.async {
                guard let self else { return }
                self.isLoading = false

                if let taskError {
                    self.error = taskError.localizedDescription
                    return
                }
                guard let data else {
                    self.error = "No data received from server."
                    return
                }
                do {
                    self.concerts = try JSONDecoder().decode([Concert].self, from: data)
                } catch {
                    self.error = "Failed to decode response: \(error.localizedDescription)"
                }
            }
        }.resume()
    }
}
