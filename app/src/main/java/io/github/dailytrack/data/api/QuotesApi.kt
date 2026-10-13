/*
 * Copyright 2024 Soul Track Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.github.dailytrack.data.api

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

data class Quote(
    val text: String,
    val author: String,
    val best: Boolean = false
)

object QuotesApi {
    private const val PREFS_NAME = "soultrack_quotes_prefs"
    private const val KEY_ONLINE_QUOTES = "online_quotes"
    private const val KEY_API_ENABLED = "api_enabled"
    private const val KEY_LAST_FETCH = "last_online_fetch"
    private const val KEY_SHOWN_QUOTES = "shown_quotes"
    private const val KEY_DISPLAY_QUOTE = "display_quote"
    private const val KEY_DISPLAY_AUTHOR = "display_author"
    private const val KEY_DISPLAY_TOKEN = "display_token"

    private const val FETCH_INTERVAL_MS = 3L * 24 * 60 * 60 * 1000L
    private const val MIN_UNSHOWN_POOL = 20
    private const val MAX_SHOWN_MEMORY = 200
    private const val SHOWN_TTL_MS = 7L * 24 * 60 * 60 * 1000L

    /** Identifies this app process so quotes rotate on every reload. */
    private val processToken: String = java.util.UUID.randomUUID().toString()
    private val bestQueue: ArrayDeque<Quote> = ArrayDeque()

    private val offlineQuotes = listOf(
        // Motivation (15) - deduplicated
        Quote("The only way to do great work is to love what you do.", "Steve Jobs"),
        Quote("Believe you can and you're halfway there.", "Theodore Roosevelt"),
        Quote("Dream it. Wish it. Do it.", "Unknown"),
        Quote("Wake up with determination. Go to bed with satisfaction.", "Unknown"),
        Quote("The way to get started is to quit talking and begin doing.", "Walt Disney"),
        Quote("Do what you can, with what you have, where you are.", "Theodore Roosevelt"),
        Quote("Success doesn't just find you. You have to go out and get it.", "Unknown"),
        Quote("The harder you work for something, the greater you'll feel when you achieve it.", "Unknown"),
        Quote("Don't stop when you're tired. Stop when you're done.", "Unknown"),
        Quote("Great things never come from comfort zones.", "Unknown"),
        Quote("Your limitation—it's only your imagination.", "Unknown"),
        Quote("Push yourself, because no one else is going to do it for you.", "Unknown"),
        Quote("The future belongs to those who believe in the beauty of their dreams.", "Eleanor Roosevelt"),
        Quote("It always seems impossible until it's done.", "Nelson Mandela"),
        Quote("Act as if what you do makes a difference. It does.", "William James"),

        // Discipline & Focus (15) - deduplicated
        Quote("Discipline is the bridge between goals and accomplishment.", "Jim Rohn"),
        Quote("It does not matter how slowly you go as long as you do not stop.", "Confucius"),
        Quote("We are what we repeatedly do. Excellence, then, is not an act but a habit.", "Aristotle"),
        Quote("The secret of your future is hidden in your daily routine.", "Mike Murdock"),
        Quote("Motivation gets you going, but discipline keeps you growing.", "John C. Maxwell"),
        Quote("Focus on being productive instead of busy.", "Tim Ferriss"),
        Quote("The successful warrior is the average man, with laser-like focus.", "Bruce Lee"),
        Quote("Where focus goes, energy flows.", "Tony Robbins"),
        Quote("Concentrate all your thoughts upon the work at hand.", "Alexander Graham Bell"),
        Quote("Starve your distractions, feed your focus.", "Daniel Gable"),
        Quote("It's not that I'm so smart, it's just that I stay with problems longer.", "Albert Einstein"),
        Quote("The main thing is to keep the main thing the main thing.", "Stephen Covey"),
        Quote("Focus on progress, not perfection.", "Unknown"),
        Quote("Concentration is the secret of strength.", "Ralph Waldo Emerson"),
        Quote("Your focus determines your reality.", "George Lucas"),

        // Growth & Learning (15) - deduplicated
        Quote("The beautiful thing about learning is that nobody can take it away from you.", "B.B. King"),
        Quote("Live as if you were to die tomorrow. Learn as if you were to live forever.", "Mahatma Gandhi"),
        Quote("The more that you read, the more things you will know.", "Dr. Seuss"),
        Quote("An investment in knowledge pays the best interest.", "Benjamin Franklin"),
        Quote("I never lose. I either win or learn.", "Nelson Mandela"),
        Quote("The only real mistake is the one from which we learn nothing.", "Henry Ford"),
        Quote("The capacity to learn is a gift; the ability to learn is a skill; the willingness to learn is a choice.", "Brian Herbert"),
        Quote("The expert in anything was once a beginner.", "Helen Hayes"),
        Quote("If you are not willing to learn, no one can help you. If you are determined to learn, no one can stop you.", "Zig Ziglar"),
        Quote("The past is a place of reference, not a place of residence.", "Roy T. Bennett"),
        Quote("Growth is the only evidence of life.", "John Henry Newman"),
        Quote("The greatest teacher, failure is.", "Yoda"),
        Quote("In a growth mindset, challenges are exciting rather than threatening.", "Carol Dweck"),
        Quote("Anyone who stops learning is old, whether at twenty or eighty.", "Henry Ford"),
        Quote("The more I learn, the more I realize how much I don't know.", "Albert Einstein"),

        // Resilience & Perseverance (15) - deduplicated
        Quote("Success is not final, failure is not fatal: it is the courage to continue that counts.", "Winston Churchill"),
        Quote("Hardships often prepare ordinary people for an extraordinary destiny.", "C.S. Lewis"),
        Quote("The greatest glory in living lies not in never falling, but in rising every time we fall.", "Nelson Mandela"),
        Quote("When you reach the end of your rope, tie a knot in it and hang on.", "Franklin D. Roosevelt"),
        Quote("Fall seven times, stand up eight.", "Japanese Proverb"),
        Quote("The oak fought the wind and was broken, the willow bent when it must and survived.", "Robert Jordan"),
        Quote("What lies behind us and what lies before us are tiny matters compared to what lies within us.", "Ralph Waldo Emerson"),
        Quote("You may encounter many defeats, but you must not be defeated.", "Maya Angelou"),
        Quote("Tough times never last, but tough people do.", "Robert Schuller"),
        Quote("The world breaks everyone, and afterward, some are strong at the broken places.", "Ernest Hemingway"),
        Quote("Pain is temporary. Quitting lasts forever.", "Lance Armstrong"),
        Quote("A hero is an ordinary individual who finds the strength to persevere and endure in spite of overwhelming obstacles.", "Christopher Reeve"),
        Quote("It is not the mountain we conquer, but ourselves.", "Sir Edmund Hillary"),
        Quote("Perseverance is not a long race; it is many short races one after the other.", "Walter Elliot"),
        Quote("The difference between a successful person and others is not a lack of strength, not a lack of knowledge, but rather a lack in will.", "Vince Lombardi"),

        // Wisdom & Mindfulness (15) - deduplicated
        Quote("The unexamined life is not worth living.", "Socrates"),
        Quote("Knowing yourself is the beginning of all wisdom.", "Aristotle"),
        Quote("The only true wisdom is in knowing you know nothing.", "Socrates"),
        Quote("Yesterday I was clever, so I wanted to change the world. Today I am wise, so I am changing myself.", "Rumi"),
        Quote("Quiet the mind, and the soul will speak.", "Ma Jaya Sati Bhagavati"),
        Quote("The mind is everything. What you think you become.", "Buddha"),
        Quote("Your calm mind is the ultimate weapon against your challenges.", "Bryant McGill"),
        Quote("Almost everything will work again if you unplug it for a few minutes, including you.", "Anne Lamott"),
        Quote("Feelings come and go like clouds in a windy sky. Conscious breathing is my anchor.", "Thich Nhat Hanh"),
        Quote("The present moment is filled with joy and happiness. If you are attentive, you will see it.", "Thich Nhat Hanh"),
        Quote("Do not dwell in the past, do not dream of the future, concentrate the mind on the present moment.", "Buddha"),
        Quote("You should sit in meditation for twenty minutes a day, unless you're too busy; then you should sit for an hour.", "Zen Proverb"),
        Quote("Simplicity is the ultimate sophistication.", "Leonardo da Vinci"),
        Quote("The greatest weapon against stress is our ability to choose one thought over another.", "William James"),
        Quote("Mindfulness is a way of befriending ourselves and our experience.", "Jon Kabat-Zinn"),

        // Courage & Action (15) - deduplicated
        Quote("Life shrinks or expands in proportion to one's courage.", "Anaïs Nin"),
        Quote("You miss 100% of the shots you don't take.", "Wayne Gretzky"),
        Quote("Inaction breeds doubt and fear. Action breeds confidence and courage.", "Dale Carnegie"),
        Quote("Do one thing every day that scares you.", "Eleanor Roosevelt"),
        Quote("Courage is not the absence of fear, but the triumph over it.", "Nelson Mandela"),
        Quote("He who is not courageous enough to take risks will accomplish nothing in life.", "Muhammad Ali"),
        Quote("The secret of change is to focus all of your energy not on fighting the old, but on building the new.", "Socrates"),
        Quote("Well done is better than well said.", "Benjamin Franklin"),
        Quote("Action is the foundational key to all success.", "Pablo Picasso"),
        Quote("The best time to plant a tree was 20 years ago. The second best time is now.", "Chinese Proverb"),
        Quote("A journey of a thousand miles begins with a single step.", "Lao Tzu"),
        Quote("You don't have to be great to start, but you have to start to be great.", "Zig Ziglar"),
        Quote("The best revenge is massive success.", "Frank Sinatra"),
        Quote("Don't wait for opportunity. Create it.", "Unknown"),
        Quote("The best way to predict the future is to create it.", "Peter Drucker"),

        // Success & Achievement (15) - deduplicated
        Quote("Success usually comes to those who are too busy to be looking for it.", "Henry David Thoreau"),
        Quote("Don't be afraid to give up the good to go for the great.", "John D. Rockefeller"),
        Quote("I find that the harder I work, the more luck I seem to have.", "Thomas Jefferson"),
        Quote("Success is not the key to happiness. Happiness is the key to success.", "Albert Schweitzer"),
        Quote("The way to achieve your own success is to help somebody else get it first.", "Tina L. Anderson"),
        Quote("Success is a lousy teacher. It seduces smart people into thinking they can't lose.", "Bill Gates"),
        Quote("Success is the sum of small efforts, repeated day in and day out.", "Robert Collier"),
        Quote("There are no shortcuts to any place worth going.", "Beverly Sills"),
        Quote("Success is not about how much money you make, but about the difference you make in people's lives.", "Michelle Obama"),
        Quote("The only limit to our realization of tomorrow will be our doubts of today.", "Franklin D. Roosevelt"),
        Quote("Success is getting what you want. Happiness is wanting what you get.", "Dale Carnegie"),
        Quote("If you really look closely, most overnight successes took a long time.", "Steve Jobs"),
        Quote("Success is how high you bounce when you hit bottom.", "George S. Patton"),
        Quote("Success is never owned, it is only rented—and the rent is due every day.", "Rory Vaden"),
        Quote("Success is the sum of a small repeated effort repeated day in and day out.", "Robert Collier"),

        // Productivity & Time (15) - deduplicated
        Quote("Time is what we want most, but what we use worst.", "William Penn"),
        Quote("Lost time is never found again.", "Benjamin Franklin"),
        Quote("The key is not to prioritize your schedule, but to schedule your priorities.", "Stephen Covey"),
        Quote("Productivity is never an accident. It is the result of a commitment to excellence.", "Paul J. Meyer"),
        Quote("Amateurs sit and wait for inspiration, the rest of us just get up and go to work.", "Stephen King"),
        Quote("The difference between successful people and really successful people is that really successful people say no to almost everything.", "Warren Buffett"),
        Quote("Efficiency is doing things right; effectiveness is doing the right things.", "Peter Drucker"),
        Quote("You don't need more time, you need fewer distractions.", "Unknown"),
        Quote("Start where you are. Use what you have. Do what you can.", "Arthur Ashe"),
        Quote("The secret of getting ahead is getting started. The secret of getting started is breaking your complex tasks into small tasks.", "Mark Twain"),
        Quote("Until we can manage time, we can manage nothing else.", "Peter Drucker"),
        Quote("Either you run the day or the day runs you.", "Jim Rohn"),
        Quote("Your future is created by what you do today, not tomorrow.", "Robert Kiyosaki"),
        Quote("The bad news is time flies. The good news is you're the pilot.", "Michael Altshuler"),
        Quote("Time is the most valuable thing a man can spend.", "Theophrastus"),

        // Leadership & Influence (15) - deduplicated
        Quote("The greatest leader is not the one who does the greatest things, but the one who gets people to do the greatest things.", "Ronald Reagan"),
        Quote("A leader is one who knows the way, goes the way, and shows the way.", "John C. Maxwell"),
        Quote("The ultimate measure of a man is not where he stands in moments of comfort, but where he stands at times of challenge.", "Martin Luther King Jr."),
        Quote("Before you are a leader, success is all about growing yourself. When you become a leader, success is all about growing others.", "Jack Welch"),
        Quote("Management is doing things right; leadership is doing the right things.", "Peter Drucker"),
        Quote("People don't care how much you know until they know how much you care.", "Theodore Roosevelt"),
        Quote("The strength of the team is each individual member. The strength of each member is the team.", "Phil Jackson"),
        Quote("Leadership is the art of getting someone else to do something you want done because he wants to do it.", "Dwight D. Eisenhower"),
        Quote("Leadership is not about being in charge. It is about taking care of those in your charge.", "Simon Sinek"),
        Quote("The task of leadership is not to put greatness into people, but to elicit it, for the greatness is there already.", "John Buchan"),
        Quote("A leader is best when people barely know he exists, when his work is done, his aim fulfilled, they will say: we did it ourselves.", "Lao Tzu"),
        Quote("Leadership and learning are indispensable to each other.", "John F. Kennedy"),
        Quote("He who has never learned to obey cannot be a good commander.", "Aristotle"),
        Quote("The price of greatness is responsibility.", "Winston Churchill"),
        Quote("Leadership is the capacity to translate vision into reality.", "Warren Bennis"),

        // Creativity & Innovation (15) - deduplicated
        Quote("Creativity is intelligence having fun.", "Albert Einstein"),
        Quote("The chief enemy of creativity is good sense.", "Pablo Picasso"),
        Quote("Logic will get you from A to B. Imagination will take you everywhere.", "Albert Einstein"),
        Quote("Creativity takes courage.", "Henri Matisse"),
        Quote("The creative adult is the child who survived.", "Ursula K. Le Guin"),
        Quote("You can't use up creativity. The more you use, the more you have.", "Maya Angelou"),
        Quote("The desire to create is one of the deepest yearnings of the human soul.", "Dieter F. Uchtdorf"),
        Quote("Imagination is the beginning of creation.", "George Bernard Shaw"),
        Quote("Creativity is thinking up new things. Innovation is doing new things.", "Theodore Levitt"),
        Quote("Don't think. Thinking is the enemy of creativity.", "Ray Bradbury"),
        Quote("Creativity involves breaking out of established patterns in order to look at things in a different way.", "Edward de Bono"),
        Quote("The true sign of intelligence is not knowledge but imagination.", "Albert Einstein"),
        Quote("To create, you must first destroy. Every act of creation is first an act of destruction.", "Pablo Picasso"),
        Quote("If you're not prepared to be wrong, you'll never come up with anything original.", "Ken Robinson"),
        Quote("Creativity is the residue of time wasted.", "Albert Einstein"),

        // Patience & Consistency (15) - deduplicated
        Quote("Patience is not simply the ability to wait—it's how we behave while we're waiting.", "Joyce Meyer"),
        Quote("The miracle is this: the more we share the more we have.", "Leonard Nimoy"),
        Quote("Consistency is what transforms average into excellence.", "Unknown"),
        Quote("Small disciplines repeated with consistency every day lead to great achievements.", "John C. Maxwell"),
        Quote("Rome wasn't built in a day, but they worked on it every single day.", "Unknown"),
        Quote("Patience is a conquering virtue.", "Geoffrey Chaucer"),
        Quote("The man who moves a mountain begins by carrying away small stones.", "Confucius"),
        Quote("With patience and persistence, even the smallest action can change the world.", "Unknown"),
        Quote("Consistency is the true foundation of trust.", "Roy T. Bennett"),
        Quote("Be not afraid of growing slowly; be afraid only of standing still.", "Chinese Proverb"),
        Quote("The key to everything is patience. You get the chicken by hatching the egg, not by smashing it.", "Arnold H. Glasgow"),
        Quote("Patience and perseverance have a magical effect before which difficulties disappear.", "Thomas Jefferson"),
        Quote("Success is neither magical nor mysterious. Success is the natural consequence of consistently applying basic fundamentals.", "Jim Rohn"),
        Quote("It's not what we do once in a while that shapes our lives, but what we do consistently.", "Tony Robbins"),
        Quote("Patience is bitter, but its fruit is sweet.", "Jean-Jacques Rousseau"),

        // Gratitude & Positivity (15) - deduplicated
        Quote("Gratitude turns what we have into enough.", "Melody Beattie"),
        Quote("The more grateful I am, the more beauty I see.", "Mary Davis"),
        Quote("Joy is the simplest form of gratitude.", "Karl Barth"),
        Quote("Appreciation is a wonderful thing. It makes what is excellent in others belong to us as well.", "Voltaire"),
        Quote("When you are grateful, fear disappears and abundance appears.", "Tony Robbins"),
        Quote("Let us be grateful to the people who make us happy.", "Marcel Proust"),
        Quote("Gratitude is not only the greatest of virtues, but the parent of all others.", "Cicero"),
        Quote("No duty is more urgent than that of returning thanks.", "James Allen"),
        Quote("Gratitude changes the pangs of memory into a tranquil joy.", "Dietrich Bonhoeffer"),
        Quote("When I started counting my blessings, my whole life turned around.", "Willie Nelson"),
        Quote("Feeling gratitude and not expressing it is like wrapping a present and not giving it.", "William Arthur Ward"),
        Quote("The soul that gives thanks can find comfort in everything.", "Hannah Whitall Smith"),
        Quote("Enjoy the little things, for one day you may look back and realize they were the big things.", "Robert Brault"),
        Quote("Gratitude is the fairest blossom which springs from the soul.", "Henry Ward Beecher"),
        Quote("Be thankful for what you have; you'll end up having more.", "Oprah Winfrey"),

        // Failure & Setbacks (15) - deduplicated
        Quote("I have not failed. I've just found 10,000 ways that won't work.", "Thomas Edison"),
        Quote("Failure is not the opposite of success: it's part of success.", "Arianna Huffington"),
        Quote("Every adversity, every failure, every heartache carries with it the seed of an equal or greater benefit.", "Napoleon Hill"),
        Quote("Failure is another stepping stone to greatness.", "Oprah Winfrey"),
        Quote("There is no innovation and creativity without failure.", "Brené Brown"),
        Quote("The only real failure in life is not to be true to the best one knows.", "Buddha"),
        Quote("Failure is the tuition you pay for success.", "Walter Brunell"),
        Quote("It is impossible to live without failing at something, unless you live so cautiously that you might as well not have lived at all.", "J.K. Rowling"),
        Quote("Failure isn't fatal, but failure to change might be.", "John Wooden"),
        Quote("You don't learn to walk by following rules. You learn by doing, and by falling over.", "Richard Branson"),
        Quote("Rock bottom became the solid foundation on which I rebuilt my life.", "J.K. Rowling"),
        Quote("There is only one thing that makes a dream impossible to achieve: the fear of failure.", "Paulo Coelho"),
        Quote("I can accept failure, everyone fails at something. But I can't accept not trying.", "Michael Jordan"),
        Quote("Failure is not falling down but refusing to get up.", "Chinese Proverb"),
        Quote("Every failure is a lesson learned.", "Unknown"),

        // Inner Strength & Self-Belief (15) - deduplicated
        Quote("No one can make you feel inferior without your consent.", "Eleanor Roosevelt"),
        Quote("The most terrifying thing is to accept oneself completely.", "Carl Jung"),
        Quote("You yourself, as much as anybody in the entire universe, deserve your love and affection.", "Buddha"),
        Quote("To be yourself in a world that is constantly trying to make you something else is the greatest accomplishment.", "Ralph Waldo Emerson"),
        Quote("The only person you are destined to become is the person you decide to be.", "Ralph Waldo Emerson"),
        Quote("Be who you are and say what you feel, because those who mind don't matter, and those who matter don't mind.", "Dr. Seuss"),
        Quote("Believe in yourself and all that you are. Know that there is something inside you that is greater than any obstacle.", "Christian D. Larson"),
        Quote("Self-confidence is the first requisite to great undertakings.", "Samuel Johnson"),
        Quote("You have been criticizing yourself for years, and it hasn't worked. Try approving of yourself and see what happens.", "Louise Hay"),
        Quote("If you hear a voice within you say you cannot paint, then by all means paint, and that voice will be silenced.", "Vincent van Gogh"),
        Quote("Trust yourself. You know more than you think you do.", "Benjamin Spock"),
        Quote("Our greatest weakness lies in giving up. The most certain way to try again is to try one more time.", "Thomas Edison"),
        Quote("Low self-esteem is like driving through life with your hand-break on.", "Maxwell Maltz"),
        Quote("Confidence comes from discipline and training.", "Robert Kiyosaki"),
        Quote("You are enough just as you are.", "Meghan Markle"),

        // Balance & Life (15) - deduplicated
        Quote("Life is what happens when you're busy making other plans.", "John Lennon"),
        Quote("In the end, it's not the years in your life that count. It's the life in your years.", "Abraham Lincoln"),
        Quote("The purpose of our lives is to be happy.", "Dalai Lama"),
        Quote("Life is really simple, but we insist on making it complicated.", "Confucius"),
        Quote("In order to write about life first you must live it.", "Ernest Hemingway"),
        Quote("The biggest adventure you can take is to live the life of your dreams.", "Oprah Winfrey"),
        Quote("Life is a long lesson in humility.", "James M. Barrie"),
        Quote("Turn your wounds into wisdom.", "Oprah Winfrey"),
        Quote("Not life, but good life, is to be chiefly valued.", "Socrates"),
        Quote("The greatest wealth is to live content with little.", "Plato"),
        Quote("Only a life lived for others is a life worthwhile.", "Albert Einstein"),
        Quote("Life is not measured by the number of breaths we take, but by the moments that take our breath away.", "Maya Angelou"),
        Quote("Spread love everywhere you go. Let no one ever come to you without leaving happier.", "Mother Teresa"),
        Quote("Always remember that you are absolutely unique. Just like everyone else.", "Margaret Mead"),
        Quote("In three words I can sum up everything I've learned about life: it goes on.", "Robert Frost"),

        // Change & Adaptability (15) - deduplicated
        Quote("Be the change that you wish to see in the world.", "Mahatma Gandhi"),
        Quote("Change is the law of life. And those who look only to the past or present are certain to miss the future.", "John F. Kennedy"),
        Quote("If you don't like something, change it. If you can't change it, change your attitude.", "Maya Angelou"),
        Quote("Progress is impossible without change, and those who cannot change their minds cannot change anything.", "George Bernard Shaw"),
        Quote("Change your thoughts and you change your world.", "Norman Vincent Peale"),
        Quote("The measure of intelligence is the ability to change.", "Albert Einstein"),
        Quote("They always say time changes things, but you actually have to change them yourself.", "Andy Warhol"),
        Quote("Every moment is a fresh beginning.", "T.S. Eliot"),
        Quote("The only way to make sense out of change is to plunge into it, move with it, and join the dance.", "Alan Watts"),
        Quote("Change is never painful, only the resistance to change is painful.", "Buddha"),
        Quote("New beginnings are often disguised as painful endings.", "Lao Tzu"),
        Quote("I am not what happened to me. I am what I choose to become.", "Carl Jung"),
        Quote("If we don't change, we don't grow. If we don't grow, we aren't really living.", "Gail Sheehy"),
        Quote("You can't build a reputation on what you're going to do.", "Henry Ford"),
        Quote("The measure of a man is what he does with power.", "Plato"),

        // Bhagavad Gita (15)
        Quote("You have a right to your actions, but never to your actions' fruits.", "Bhagavad Gita", best = true),
        Quote("The mind is restless and difficult to restrain, but it is subdued by practice.", "Bhagavad Gita", best = true),
        Quote("When meditation is mastered, the mind is unwavering like the flame of a lamp in a windless place.", "Bhagavad Gita", best = true),
        Quote("A person can rise through the efforts of their own mind, or draw themselves down in the same manner.", "Bhagavad Gita", best = true),
        Quote("No one who does good work will ever come to a bad end.", "Bhagavad Gita", best = true),
        Quote("The soul is neither born, nor does it ever die.", "Bhagavad Gita", best = true),
        Quote("As a person puts on new garments, giving up old ones, the soul accepts new bodies.", "Bhagavad Gita", best = true),
        Quote("Set thy heart upon thy work, but never on its reward.", "Bhagavad Gita", best = true),
        Quote("Man is made by his belief. As he believes, so he is.", "Bhagavad Gita", best = true),
        Quote("There is neither this world, nor the world beyond, nor happiness for the one who doubts.", "Bhagavad Gita", best = true),
        Quote("Better is one's own duty, though imperfectly performed, than the duty of another well performed.", "Bhagavad Gita", best = true),
        Quote("Calmness, gentleness, silence, self-restraint and purity: these are the disciplines of the mind.", "Bhagavad Gita", best = true),
        Quote("For one who has conquered the mind, the mind is the best of friends; for one who has failed, it remains the greatest enemy.", "Bhagavad Gita", best = true),
        Quote("Whatever happened, happened for the good. Whatever is happening, is happening for the good.", "Bhagavad Gita", best = true),
        Quote("The wise see knowledge and action as one.", "Bhagavad Gita", best = true),

        // Stoics & Classics (15)
        Quote("The impediment to action advances action. What stands in the way becomes the way.", "Marcus Aurelius", best = true),
        Quote("Waste no more time arguing about what a good man should be. Be one.", "Marcus Aurelius", best = true),
        Quote("The happiness of your life depends upon the quality of your thoughts.", "Marcus Aurelius", best = true),
        Quote("The soul becomes dyed with the color of its thoughts.", "Marcus Aurelius", best = true),
        Quote("If it is not right, do not do it. If it is not true, do not say it.", "Marcus Aurelius", best = true),
        Quote("It is not that we have a short time to live, but that we waste a great deal of it.", "Seneca", best = true),
        Quote("Luck is what happens when preparation meets opportunity.", "Seneca", best = true),
        Quote("We suffer more often in imagination than in reality.", "Seneca", best = true),
        Quote("Begin at once to live, and count each separate day as a separate life.", "Seneca", best = true),
        Quote("Difficulties strengthen the mind, as labor does the body.", "Seneca", best = true),
        Quote("It's not what happens to you, but how you react to it that matters.", "Epictetus", best = true),
        Quote("He is a wise man who does not grieve for the things which he has not, but rejoices for those which he has.", "Epictetus", best = true),
        Quote("No man is free who is not master of himself.", "Epictetus", best = true),
        Quote("First say to yourself what you would be, and then do what you have to do.", "Epictetus", best = true),
        Quote("The greater the difficulty, the more glory in surmounting it.", "Epicurus", best = true),

        // World Books (15)
        Quote("The two most powerful warriors are patience and time.", "Leo Tolstoy", best = true),
        Quote("Taking a new step, uttering a new word, is what people fear most.", "Fyodor Dostoevsky", best = true),
        Quote("You can't cross the sea merely by standing and staring at the water.", "Rabindranath Tagore", best = true),
        Quote("Work is love made visible.", "Kahlil Gibran", best = true),
        Quote("Between stimulus and response there is a space. In that space is our power to choose.", "Viktor Frankl", best = true),
        Quote("When we are no longer able to change a situation, we are challenged to change ourselves.", "Viktor Frankl", best = true),
        Quote("The only way out is through.", "Robert Frost", best = true),
        Quote("All we have to decide is what to do with the time that is given us.", "J.R.R. Tolkien", best = true),
        Quote("Not all those who wander are lost.", "J.R.R. Tolkien", best = true),
        Quote("It is our choices that show what we truly are, far more than our abilities.", "J.K. Rowling", best = true),
        Quote("Happiness can be found even in the darkest of times, if one only remembers to turn on the light.", "J.K. Rowling", best = true),
        Quote("We are all in the gutter, but some of us are looking at the stars.", "Oscar Wilde", best = true),
        Quote("Be yourself; everyone else is already taken.", "Oscar Wilde", best = true),
        Quote("In the midst of chaos, there is also opportunity.", "Sun Tzu", best = true),
        Quote("I am no bird; and no net ensnares me.", "Charlotte Brontë", best = true),

        // Bhagavad Gita - more of the most quoted verses
        Quote("One who sees inaction in action, and action in inaction, is intelligent among men.", "Bhagavad Gita", best = true),
        Quote("Change is the law of the universe. What you call death is but a change of body.", "Bhagavad Gita", best = true),
        Quote("The mind acts like an enemy for those who do not control it.", "Bhagavad Gita", best = true),
        Quote("Steadiness in yoga is excellence in action.", "Bhagavad Gita", best = true),
        Quote("Heat and cold, pleasure and pain come and go. Endure them patiently.", "Bhagavad Gita", best = true),
        Quote("Whatever you do, do it as an offering, and without attachment to the result.", "Bhagavad Gita", best = true),
        Quote("The wise see the same self in a learned Brahmin, a cow, an elephant, a dog and an outcast.", "Bhagavad Gita", best = true),
        Quote("Fearlessness, purity, self-control, charity, modesty, austerity: these are the qualities of the born divine.", "Bhagavad Gita", best = true),
        Quote("Among thousands, only one strives for perfection; and of those, few know Me.", "Bhagavad Gita", best = true),
        Quote("Ponder the sublime wisdom of the Gita every day; it is the greatest secret of all.", "Bhagavad Gita", best = true),
        Quote("The soul wears the body as a garment, and casts it off when it is worn out.", "Bhagavad Gita", best = true),
        Quote("He who sees Me in everything and everything in Me, never turns away from Me.", "Bhagavad Gita", best = true),

        // Best lines from the world's books
        Quote("The only thing we have to fear is fear itself.", "Franklin D. Roosevelt", best = true),
        Quote("In the middle of difficulty lies opportunity.", "Albert Einstein", best = true),
        Quote("Imagination is more important than knowledge.", "Albert Einstein", best = true),
        Quote("I have no special talents. I am only passionately curious.", "Albert Einstein", best = true),
        Quote("Try not to become a man of success, but rather a man of value.", "Albert Einstein", best = true),
        Quote("The unexamined life is not worth living.", "Socrates", best = true),
        Quote("The only true wisdom is in knowing you know nothing.", "Socrates", best = true),
        Quote("We are what we repeatedly do. Excellence, then, is not an act but a habit.", "Aristotle", best = true),
        Quote("Knowing yourself is the beginning of all wisdom.", "Aristotle", best = true),
        Quote("He who has a why to live can bear almost any how.", "Friedrich Nietzsche", best = true),
        Quote("That which does not kill us makes us stronger.", "Friedrich Nietzsche", best = true),
        Quote("What you think, you become.", "Buddha", best = true),
        Quote("Peace comes from within. Do not seek it without.", "Buddha", best = true),
        Quote("Happiness is not something ready made. It comes from your own actions.", "Dalai Lama", best = true),
        Quote("Be kind whenever possible. It is always possible.", "Dalai Lama", best = true),
        Quote("The best time to plant a tree was 20 years ago. The second best time is now.", "Chinese Proverb", best = true),
        Quote("An eye for an eye will only make the whole world blind.", "Mahatma Gandhi", best = true),
        Quote("In a gentle way, you can shake the world.", "Mahatma Gandhi", best = true),
        Quote("Arise, awake, and stop not till the goal is reached.", "Swami Vivekananda", best = true),
        Quote("We are what our thoughts have made us; so take care of what you think.", "Swami Vivekananda", best = true),
        Quote("The wound is the place where the Light enters you.", "Rumi", best = true),
        Quote("What you seek is seeking you.", "Rumi", best = true),
        Quote("Yesterday I was clever, so I wanted to change the world. Today I am wise, so I am changing myself.", "Rumi", best = true),
        Quote("The secret of getting ahead is getting started.", "Mark Twain", best = true),
        Quote("Well done is better than well said.", "Benjamin Franklin", best = true),
        Quote("You miss 100% of the shots you don't take.", "Wayne Gretzky", best = true),
        Quote("Whether you think you can or you think you can't, you're right.", "Henry Ford", best = true),
        Quote("Simplicity is the ultimate sophistication.", "Leonardo da Vinci", best = true),
        Quote("Learning never exhausts the mind.", "Leonardo da Vinci", best = true),
        Quote("Your time is limited, so don't waste it living someone else's life.", "Steve Jobs", best = true),
        Quote("Discipline equals freedom.", "Jocko Willink", best = true),
        Quote("How we spend our days is, of course, how we spend our lives.", "Annie Dillard", best = true)
    )

    private var onlineQuotes: List<Quote> = emptyList()
    private var prefs: SharedPreferences? = null
    private val shownQuotes = mutableSetOf<String>()
    private var quoteQueue = mutableListOf<Quote>()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadOnlineQuotesFromPrefs()
        loadShownQuotes()
    }

    private fun loadOnlineQuotesFromPrefs() {
        val json = prefs?.getString(KEY_ONLINE_QUOTES, null) ?: return
        try {
            val arr = JSONArray(json)
            onlineQuotes = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Quote(obj.getString("text"), obj.getString("author"))
            }
        } catch (_: Exception) { }
    }

    private fun loadShownQuotes() {
        val json = prefs?.getString(KEY_SHOWN_QUOTES, null) ?: return
        try {
            val obj = JSONObject(json)
            val timestamp = obj.optLong("timestamp", 0L)
            val now = System.currentTimeMillis()
            if (now - timestamp < SHOWN_TTL_MS) {
                val arr = obj.optJSONArray("quotes") ?: return
                shownQuotes.clear()
                for (i in 0 until arr.length()) {
                    shownQuotes.add(arr.getString(i))
                }
            } else {
                shownQuotes.clear()
                prefs?.edit()?.remove(KEY_SHOWN_QUOTES)?.apply()
            }
        } catch (_: Exception) {
            shownQuotes.clear()
        }
    }

    private fun rememberShown(text: String) {
        shownQuotes.add(text)
        while (shownQuotes.size > MAX_SHOWN_MEMORY) {
            val oldest = shownQuotes.iterator()
            if (oldest.hasNext()) {
                oldest.next()
                oldest.remove()
            } else break
        }
        saveShownQuotes()
    }

    private fun saveShownQuotes() {
        val arr = JSONArray()
        shownQuotes.forEach { arr.put(it) }
        val obj = JSONObject().apply {
            put("quotes", arr)
            put("timestamp", System.currentTimeMillis())
        }
        prefs?.edit()?.putString("shown_quotes", obj.toString())?.apply()
    }

    private fun getTimeOfDay(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "morning"
            hour < 17 -> "afternoon"
            else -> "evening"
        }
    }

    private fun refillQueue() {
        val all = (offlineQuotes + onlineQuotes).filter { it.text !in shownQuotes }
        quoteQueue.clear()
        quoteQueue.addAll(all.shuffled())
        if (quoteQueue.isEmpty()) {
            shownQuotes.clear()
            quoteQueue.addAll((offlineQuotes + onlineQuotes).shuffled())
        }
    }

    private fun getWeightedRandom(quotes: List<Quote>): Quote {
        if (quotes.isEmpty()) return offlineQuotes.random()
        val weights = quotes.map { q ->
            val authorCount = quotes.count { it.author == q.author }
            val categoryCount = offlineQuotes.count { it.author == q.author }
            1.0f / (authorCount.toFloat().coerceAtLeast(1f)) * (categoryCount.toFloat().coerceAtLeast(1f))
        }
        val totalWeight = weights.sum()
        var random = Math.random() * totalWeight
        for (i in quotes.indices) {
            random -= weights[i]
            if (random <= 0) return quotes[i]
        }
        return quotes.last()
    }

    suspend fun getRandomQuote(): Quote {
        if (quoteQueue.isEmpty()) refillQueue()
        val quote = quoteQueue.removeFirstOrNull() ?: offlineQuotes.random()
        rememberShown(quote.text)
        return quote
    }

    suspend fun getQuoteByCategory(categoryType: String): Quote {
        val categoryKeywords = when (categoryType.uppercase()) {
            "PRODUCTIVE", "LEARNING" -> listOf("focus", "work", "productivity", "discipline", "success", "goal")
            "EXERCISE" -> listOf("exercise", "health", "body", "strength", "movement", "energy")
            "WASTED" -> listOf("time", "distraction", "purpose", "discipline", "focus")
            "SOCIAL" -> listOf("people", "relationship", "connection", "team", "community")
            else -> emptyList()
        }
        if (categoryKeywords.isEmpty()) return getRandomQuote()
        val all = (offlineQuotes + onlineQuotes).filter { q ->
            categoryKeywords.any { kw -> q.text.contains(kw, ignoreCase = true) }
        }
        val filtered = all.filter { it.text !in shownQuotes }
        val quote = if (filtered.isNotEmpty()) getWeightedRandom(filtered) else getRandomQuote()
        rememberShown(quote.text)
        return quote
    }

    suspend fun getTimedQuote(): Quote {
        val timeOfDay = getTimeOfDay()
        val timeKeywords = when (timeOfDay) {
            "morning" -> listOf(
                "morning", "day", "begin", "start", "wake", "dawn", "rise", "today",
                "new", "opportunity", "action", "do", "go", "energy", "courage"
            )
            "afternoon" -> listOf(
                "work", "focus", "productivity", "progress", "momentum", "discipline",
                "persist", "continue", "effort", "strength", "courage", "action", "do"
            )
            "evening" -> listOf(
                "rest", "reflect", "peace", "calm", "gratitude", "sleep", "soul",
                "mind", "wisdom", "content", "quiet", "still", "thank"
            )
            else -> emptyList()
        }
        val all = (offlineQuotes + onlineQuotes).filter { q ->
            timeKeywords.any { kw -> q.text.contains(kw, ignoreCase = true) }
        }
        val filtered = all.filter { it.text !in shownQuotes }
        val quote = when {
            filtered.isNotEmpty() -> getWeightedRandom(filtered)
            all.isNotEmpty() -> getWeightedRandom(all)
            else -> getRandomQuote()
        }
        rememberShown(quote.text)
        return quote
    }

    fun isApiEnabled(): Boolean {
        return prefs?.getBoolean(KEY_API_ENABLED, true) ?: true
    }

    fun setApiEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_API_ENABLED, enabled)?.apply()
    }

    fun getLastFetchTime(): Long {
        return prefs?.getLong(KEY_LAST_FETCH, 0L) ?: 0L
    }

    fun getUnshownPoolSize(): Int {
        return (offlineQuotes + onlineQuotes).count { it.text !in shownQuotes }
    }

    fun shouldRefresh(now: Long = System.currentTimeMillis()): Boolean {
        if (!isApiEnabled()) return false
        val last = getLastFetchTime()
        if (last <= 0L) return true
        if (now - last >= FETCH_INTERVAL_MS) return true
        return getUnshownPoolSize() < MIN_UNSHOWN_POOL
    }

    suspend fun maybeRefreshQuotes(force: Boolean = false): List<Quote> {
        if (!force && !shouldRefresh()) return onlineQuotes
        if (!isApiEnabled() && !force) return onlineQuotes
        val fetched = fetchOnlineQuotes()
        if (fetched.isNotEmpty()) {
            prefs?.edit()?.putLong(KEY_LAST_FETCH, System.currentTimeMillis())?.apply()
            bestQueue.clear()
        }
        return fetched
    }

    /** Number of flagship quotes (Bhagavad Gita, stoics, world books). */
    fun getBestQuotesCount(): Int =
        offlineQuotes.count { it.best } + onlineQuotes.count { it.best }

    private fun refillBestQueue() {
        val best = (offlineQuotes + onlineQuotes).filter { it.best }
        val fresh = best.filter { it.text !in shownQuotes }.shuffled()
        bestQueue.clear()
        bestQueue.addAll(if (fresh.isNotEmpty()) fresh else best.shuffled())
    }

    /**
     * Rotates in a fresh flagship quote. Called once per app launch so every
     * reload surfaces a new "best of the world" line instead of yesterday's.
     */
    suspend fun rotateDisplayQuote(): Quote {
        if (bestQueue.isEmpty()) refillBestQueue()
        val quote = bestQueue.removeFirstOrNull() ?: getRandomQuote()
        rememberShown(quote.text)
        prefs?.edit()
            ?.putString(KEY_DISPLAY_QUOTE, quote.text)
            ?.putString(KEY_DISPLAY_AUTHOR, quote.author)
            ?.putString(KEY_DISPLAY_TOKEN, processToken)
            ?.apply()
        return quote
    }

    fun getDisplayQuote(): Quote? {
        val text = prefs?.getString(KEY_DISPLAY_QUOTE, null) ?: return null
        if (text.isBlank()) return null
        return Quote(text, prefs?.getString(KEY_DISPLAY_AUTHOR, "") ?: "")
    }

    /** True when this process has already rotated the quote for this launch. */
    fun hasFreshDisplayQuote(): Boolean =
        prefs?.getString(KEY_DISPLAY_TOKEN, null) == processToken

    suspend fun fetchOnlineQuotes(): List<Quote> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://zenquotes.io/api/quotes")
                val connection = url.openConnection()
                val inputStream = connection.getInputStream()
                val response = inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(response)
                val quotes = (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    Quote(obj.getString("q"), obj.getString("a"))
                }
                onlineQuotes = quotes
                saveOnlineQuotesToPrefs(quotes)
                quotes
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    private fun saveOnlineQuotesToPrefs(quotes: List<Quote>) {
        val arr = JSONArray()
        quotes.forEach { q ->
            arr.put(JSONObject().apply {
                put("text", q.text)
                put("author", q.author)
            })
        }
        prefs?.edit()?.putString(KEY_ONLINE_QUOTES, arr.toString())?.apply()
    }

    fun getTotalQuotesCount(): Int = offlineQuotes.size + onlineQuotes.size
    fun getOfflineQuotesCount(): Int = offlineQuotes.size
}
