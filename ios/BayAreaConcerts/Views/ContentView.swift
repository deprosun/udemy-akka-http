import SwiftUI

struct ContentView: View {
    @StateObject private var service = ConcertService()

    var body: some View {
        NavigationView {
            Group {
                if service.isLoading {
                    ProgressView("Loading concerts…")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)

                } else if let error = service.error {
                    VStack(spacing: 16) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.system(size: 48))
                            .foregroundColor(.orange)
                        Text(error)
                            .multilineTextAlignment(.center)
                            .foregroundColor(.secondary)
                        Button("Try Again") { service.fetchConcerts() }
                            .buttonStyle(.borderedProminent)
                    }
                    .padding()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)

                } else if service.concerts.isEmpty {
                    Text("No upcoming concerts found.")
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)

                } else {
                    List(service.concerts) { concert in
                        ConcertRow(concert: concert)
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("Bay Area Concerts")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        service.fetchConcerts()
                    } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                    .disabled(service.isLoading)
                }
            }
        }
        .onAppear { service.fetchConcerts() }
    }
}
