-- Script d'insertion de 10 événements
INSERT INTO events (title, description, date_time, location, organizer_id, created_at, updated_at) 
VALUES 
    -- Conférences Tech
    (
        'Tech Conference 2024',
        'Découvrez les dernières innovations technologiques et rencontrez des experts du domaine. Au programme : IA, Cloud Computing, Cybersécurité et plus encore.',
        '2024-06-25 09:00:00',
        'Tunis, Tunisia',
        'org_001',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'AI Summit 2024',
        'Explorez le futur de l''intelligence artificielle avec des conférenciers de renommée mondiale. Démonstrations pratiques et sessions interactives.',
        '2024-07-15 10:00:00',
        'Paris, France',
        'org_002',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'Web Dev Workshop 2024',
        'Apprenez les dernières technologies web et améliorez vos compétences en développement. Focus sur React, Angular et Node.js.',
        '2024-08-10 14:00:00',
        'Lyon, France',
        'org_003',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),

    -- Événements Business
    (
        'Startup Weekend Carthage',
        'Un weekend intense pour transformer votre idée en startup. Mentoring, networking et pitchs devant des investisseurs.',
        '2024-09-20 18:00:00',
        'Carthage, Tunisia',
        'org_004',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'Digital Marketing Masterclass',
        'Une journée complète dédiée aux stratégies de marketing digital. SEO, réseaux sociaux, et publicité en ligne.',
        '2024-05-30 09:30:00',
        'Sfax, Tunisia',
        'org_005',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),

    -- Événements Formation
    (
        'Workshop DevOps',
        'Formation pratique sur les outils et pratiques DevOps. Docker, Kubernetes, CI/CD et monitoring.',
        '2024-07-05 09:00:00',
        'Sousse, Tunisia',
        'org_006',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'Data Science Bootcamp',
        'Bootcamp intensif de 3 jours sur la science des données. Python, Machine Learning et Visualisation de données.',
        '2024-08-25 09:00:00',
        'Monastir, Tunisia',
        'org_007',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),

    -- Événements Networking
    (
        'Tech Meetup Tunisia',
        'Rencontrez la communauté tech tunisienne. Présentations, discussions et networking dans une ambiance décontractée.',
        '2024-06-10 18:30:00',
        'Tunis, Tunisia',
        'org_008',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'Blockchain & Crypto Conference',
        'Tout sur la blockchain et les cryptomonnaies. Use cases, régulation et opportunités d''investissement.',
        '2024-10-15 10:00:00',
        'Hammamet, Tunisia',
        'org_009',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'Innovation Summit 2024',
        'Le plus grand rassemblement d''innovateurs en Tunisie. Startups, investisseurs et experts se rencontrent.',
        '2024-11-20 09:00:00',
        'Tunis, Tunisia',
        'org_010',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );

-- Script d'insertion de 10 événements pour 2026
INSERT INTO events (title, description, date_time, location, organizer_id, created_at, updated_at) 
VALUES 
    -- Premier Trimestre 2026
    (
        'Future Tech Summit 2026',
        'Le plus grand sommet technologique de l''année. Découvrez les innovations qui façonneront notre futur : IA quantique, métavers industriel, et robotique avancée.',
        '2026-02-15 09:00:00',
        'Tunis, Tunisia',
        'org_001',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'Cyber Security Conference',
        'Conférence internationale sur la cybersécurité avec focus sur la protection des infrastructures critiques et la sécurité de l''IA.',
        '2026-03-20 10:00:00',
        'Paris, France',
        'org_002',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),

    -- Deuxième Trimestre 2026
    (
        'Green Tech Innovation 2026',
        'Forum sur les technologies vertes et le développement durable. Solutions innovantes pour un futur éco-responsable.',
        '2026-04-10 09:30:00',
        'Sfax, Tunisia',
        'org_003',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'Digital Health Congress',
        'Congrès international sur la santé numérique. Technologies médicales avancées et télémédecine du futur.',
        '2026-05-25 08:30:00',
        'Lyon, France',
        'org_004',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),

    -- Troisième Trimestre 2026
    (
        'Space Tech Forum',
        'Forum sur les technologies spatiales et leurs applications terrestres. Exploration spatiale et innovations satellitaires.',
        '2026-07-15 10:00:00',
        'Sousse, Tunisia',
        'org_005',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'Quantum Computing Summit',
        'Sommet sur l''informatique quantique et ses applications pratiques dans l''industrie et la recherche.',
        '2026-08-30 09:00:00',
        'Monastir, Tunisia',
        'org_006',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'Smart Cities Expo 2026',
        'Exposition sur les villes intelligentes du futur. IoT, mobilité urbaine et gestion intelligente des ressources.',
        '2026-09-20 10:00:00',
        'Hammamet, Tunisia',
        'org_007',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),

    -- Quatrième Trimestre 2026
    (
        'FinTech Revolution',
        'Conférence sur l''avenir des technologies financières. Blockchain 3.0, monnaies numériques et finance décentralisée.',
        '2026-10-05 09:00:00',
        'Tunis, Tunisia',
        'org_008',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'AI & Robotics Expo',
        'Exposition internationale sur l''intelligence artificielle et la robotique. Démonstrations et conférences d''experts.',
        '2026-11-15 10:00:00',
        'Carthage, Tunisia',
        'org_009',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'Web 4.0 Conference',
        'Conférence sur le futur du web. Intelligence ambiante, réalité augmentée et expériences immersives.',
        '2026-12-10 09:00:00',
        'Paris, France',
        'org_010',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ); 