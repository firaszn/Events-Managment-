-- Suppression des données existantes
TRUNCATE TABLE events CASCADE;

-- Réinitialisation de la séquence d'ID
ALTER SEQUENCE events_id_seq RESTART WITH 1;

-- Insertion des événements
INSERT INTO events (id, title, description, event_date, location, organizer_id, created_at, updated_at)
VALUES 
    -- Événements 2024
    (1, 'Tech Summit 2024', 'Conférence sur les dernières tendances technologiques et l''innovation. Découvrez les technologies émergentes.', '2024-03-15T09:00:00', 'Tunis, Tunisia', 'org_001', '2024-01-25T14:11:44.994491', '2024-01-25T14:11:44.994491'),
    
    (2, 'DevOps Conference 2024', 'Une journée complète dédiée aux pratiques DevOps modernes et à l''automatisation des processus.', '2024-05-20T10:00:00', 'Sfax, Tunisia', 'org_002', '2024-01-25T14:11:44.994491', '2024-01-25T14:11:44.994491'),
    
    (3, 'AI & Machine Learning Day', 'Explorez les dernières avancées en IA et Machine Learning avec des experts du domaine.', '2024-07-10T09:30:00', 'Sousse, Tunisia', 'org_003', '2024-01-25T14:11:44.994491', '2024-01-25T14:11:44.994491'),
    
    (4, 'Cloud Native Summit', 'Summit sur les architectures cloud-native et les microservices. Best practices et retours d''expérience.', '2024-09-25T10:00:00', 'Monastir, Tunisia', 'org_004', '2024-01-25T14:11:44.994491', '2024-01-25T14:11:44.994491'),
    
    (5, 'Cybersecurity Forum 2024', 'Forum sur la sécurité informatique et la protection des données. Enjeux et solutions.', '2024-11-15T09:00:00', 'Hammamet, Tunisia', 'org_005', '2024-01-25T14:11:44.994491', '2024-01-25T14:11:44.994491'),

    -- Événements 2026
    (6, 'Future Tech Summit 2026', 'Le plus grand sommet technologique de l''année. Découvrez les innovations qui façonneront notre futur.', '2026-02-15T09:00:00', 'Tunis, Tunisia', 'org_006', '2025-06-25T14:11:44.994491', '2025-06-25T14:11:44.994491'),
    
    (7, 'Smart Cities Expo 2026', 'Exposition sur les villes intelligentes du futur. IoT, mobilité urbaine et gestion intelligente des ressources.', '2026-06-27T10:00:00', 'Hammamet, Tunisia', 'org_007', '2025-06-25T14:11:44.994491', '2025-06-25T14:11:44.994491'),
    
    (8, 'Quantum Computing Conference', 'Conférence sur l''informatique quantique et ses applications dans l''industrie.', '2026-08-20T10:00:00', 'Sfax, Tunisia', 'org_008', '2025-06-25T14:11:44.994491', '2025-06-25T14:11:44.994491'),
    
    (9, 'Blockchain Revolution 2026', 'Découvrez l''avenir de la blockchain et ses applications dans différents secteurs.', '2026-10-05T09:00:00', 'Sousse, Tunisia', 'org_009', '2025-06-25T14:11:44.994491', '2025-06-25T14:11:44.994491'),
    
    (10, 'Web 4.0 Conference', 'Conférence sur le futur du web. Intelligence ambiante, réalité augmentée et expériences immersives.', '2026-12-10T09:00:00', 'Monastir, Tunisia', 'org_010', '2025-06-25T14:11:44.994491', '2025-06-25T14:11:44.994491');

-- Réinitialisation de la séquence pour qu'elle continue après la plus grande valeur d'ID
SELECT setval('events_id_seq', (SELECT MAX(id) FROM events)); 