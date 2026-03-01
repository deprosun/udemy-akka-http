import SwiftUI

struct ConcertRow: View {
    let concert: Concert

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(concert.name)
                .font(.headline)

            Label(formattedDate, systemImage: "calendar")
                .font(.subheadline)
                .foregroundColor(.secondary)

            Label(concert.venue, systemImage: "mappin.and.ellipse")
                .font(.subheadline)
                .foregroundColor(.secondary)

            if let ticketURL = URL(string: concert.url) {
                Link("Get Tickets →", destination: ticketURL)
                    .font(.caption.bold())
                    .foregroundColor(.accentColor)
            }
        }
        .padding(.vertical, 8)
    }

    // Converts "2026-03-15" → "Mar 15, 2026"
    private var formattedDate: String {
        let parser = DateFormatter()
        parser.dateFormat = "yyyy-MM-dd"
        guard let date = parser.date(from: concert.date) else { return concert.date }

        let display = DateFormatter()
        display.dateStyle = .medium
        display.timeStyle = .none
        return display.string(from: date)
    }
}
