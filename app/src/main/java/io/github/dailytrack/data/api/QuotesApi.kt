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
    val author: String
)

object QuotesApi {
    private const val PREFS_NAME = "soultrack_quotes_prefs"
    private const val KEY_ONLINE_QUOTES = "online_quotes"
    private const val KEY_API_ENABLED = "api_enabled"

    private val offlineQuotes = listOf(
        // Motivation (20)
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
        Quote("What you get by achieving your goals is not as important as what you become by achieving your goals.", "Zig Ziglar"),
        Quote("You are never too old to set another goal or to dream a new dream.", "C.S. Lewis"),
        Quote("The secret of getting ahead is getting started.", "Mark Twain"),
        Quote("Don't watch the clock; do what it does. Keep going.", "Sam Levenson"),
        Quote("Everything you've ever wanted is on the other side of fear.", "George Addair"),

        // Discipline & Focus (20)
        Quote("Discipline is the bridge between goals and accomplishment.", "Jim Rohn"),
        Quote("Success is walking from failure to failure with no loss of enthusiasm.", "Winston Churchill"),
        Quote("It does not matter how slowly you go as long as you do not stop.", "Confucius"),
        Quote("We are what we repeatedly do. Excellence, then, is not an act but a habit.", "Aristotle"),
        Quote("The secret of your future is hidden in your daily routine.", "Mike Murdock"),
        Quote("Motivation gets you going, but discipline keeps you growing.", "John C. Maxwell"),
        Quote("Focus on being productive instead of busy.", "Tim Ferriss"),
        Quote("The successful warrior is the average man, with laser-like focus.", "Bruce Lee"),
        Quote("Where focus goes, energy flows.", "Tony Robbins"),
        Quote("Concentrate all your thoughts upon the work at hand.", "Alexander Graham Bell"),
        Quote("Starve your distractions, feed your focus.", "Daniel Gable"),
        Quote("You will never reach your destination if you stop and throw stones at every dog that barks.", "Winston Churchill"),
        Quote("It's not that I'm so smart, it's just that I stay with problems longer.", "Albert Einstein"),
        Quote("The main thing is to keep the main thing the main thing.", "Stephen Covey"),
        Quote("Lack of direction, not lack of time, is the problem. We all have twenty-four hour days.", "Zig Ziglar"),
        Quote("Focus on progress, not perfection.", "Unknown"),
        Quote("The key to focus is saying no to the good so you can say yes to the best.", "John C. Maxwell"),
        Quote("Concentration is the secret of strength.", "Ralph Waldo Emerson"),
        Quote("Your focus determines your reality.", "George Lucas"),
        Quote("Single-minded persistence is the mother of all skill.", "Tony Robbins"),

        // Growth & Learning (20)
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
        Quote("Learn as much as you can while you are young, since life will give you plenty of opportunities to learn later.", "Unknown"),
        Quote("The more I learn, the more I realize how much I don't know.", "Albert Einstein"),
        Quote("Learning is not attained by chance, it must be sought for with ardor and attended to with diligence.", "Abigail Adams"),
        Quote("Education is the passport to the future, for tomorrow belongs to those who prepare for it today.", "Malcolm X"),
        Quote("An investment in knowledge always pays the best interest.", "Benjamin Franklin"),
        Quote("The only thing that interferes with my learning is my education.", "Albert Einstein"),

        // Resilience & Perseverance (20)
        Quote("Success is not final, failure is not fatal: it is the courage to continue that counts.", "Winston Churchill"),
        Quote("Hardships often prepare ordinary people for an extraordinary destiny.", "C.S. Lewis"),
        Quote("The greatest glory in living lies not in never falling, but in rising every time we fall.", "Nelson Mandela"),
        Quote("When you reach the end of your rope, tie a knot in it and hang on.", "Franklin D. Roosevelt"),
        Quote("Fall seven times, stand up eight.", "Japanese Proverb"),
        Quote("The oak fought the wind and was broken, the willow bent when it must and survived.", "Robert Jordan"),
        Quote("What lies behind us and what lies before us are tiny matters compared to what lies within us.", "Ralph Waldo Emerson"),
        Quote("You may encounter many defeats, but you must not be defeated.", "Maya Angelou"),
        Quote("Failure is the condiment that gives success its flavor.", "Truman Capote"),
        Quote("Our greatest glory is not in never falling, but in rising every time we fall.", "Confucius"),
        Quote("Tough times never last, but tough people do.", "Robert Schuller"),
        Quote("The world breaks everyone, and afterward, some are strong at the broken places.", "Ernest Hemingway"),
        Quote("Pain is temporary. Quitting lasts forever.", "Lance Armstrong"),
        Quote("A hero is an ordinary individual who finds the strength to persevere and endure in spite of overwhelming obstacles.", "Christopher Reeve"),
        Quote("It is not the mountain we conquer, but ourselves.", "Sir Edmund Hillary"),
        Quote("Perseverance is not a long race; it is many short races one after the other.", "Walter Elliot"),
        Quote("The difference between a successful person and others is not a lack of strength, not a lack of knowledge, but rather a lack in will.", "Vince Lombardi"),
        Quote("Through perseverance many people win success out of what seemed destined to be a certain failure.", "Benjamin Disraeli"),
        Quote("Persistence and resilience only come from having been given the chance to work through difficult problems.", "Gever Tulley"),
        Quote("The road to success is not easy to navigate, but with hard work, drive, and passion, it's possible to achieve your dream.", "Mark Wahlberg"),

        // Wisdom & Mindfulness (20)
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
        Quote("In today's rush, we all think too much, seek too much, want too much.", "Don Miguel Ruiz"),
        Quote("You should sit in meditation for twenty minutes a day, unless you're too busy; then you should sit for an hour.", "Zen Proverb"),
        Quote("Simplicity is the ultimate sophistication.", "Leonardo da Vinci"),
        Quote("The greatest weapon against stress is our ability to choose one thought over another.", "William James"),
        Quote("Mindfulness is a way of befriending ourselves and our experience.", "Jon Kabat-Zinn"),
        Quote("The present moment is the only moment available to us, and it is the door to all moments.", "Thich Nhat Hanh"),
        Quote("Feelings come and go like clouds in a windy sky. Conscious breathing is my anchor.", "Thich Nhat Hanh"),
        Quote("In today's rush, we all think too much, seek too much, want too much, and have lost our relationship with the only thing that matters: being here.", "Unknown"),
        Quote("The soul always knows what to do to heal itself. The challenge is to silence the mind.", "Caroline Myss"),

        // Courage & Action (20)
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
        Quote("Your time is limited, don't waste it living someone else's life.", "Steve Jobs"),
        Quote("The secret of getting ahead is getting started.", "Mark Twain"),
        Quote("Action expresses priorities.", "Mahatma Gandhi"),
        Quote("It is better to take many small steps in the right direction than to make a great leap forward only to stumble backward.", "Chinese Proverb"),
        Quote("You don't need to be perfect to be amazing.", "Unknown"),
        Quote("The best way to predict the future is to create it.", "Peter Drucker"),

        // Success & Achievement (20)
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
        Quote("The road to success and the road to failure are almost exactly the same.", "Colin R. Davis"),
        Quote("Don't be distracted by criticism. Remember, the only taste of success some people get is when they take a bite out of you.", "Zig Ziglar"),
        Quote("Success is how high you bounce when you hit bottom.", "George S. Patton"),
        Quote("Success is not the result of spontaneous combustion. You must set yourself on fire.", "Arnold H. Glasow"),
        Quote("Success is not final, failure is not fatal: it is the courage to continue that counts.", "Winston Churchill"),
        Quote("Success is walking from failure to failure with no loss of enthusiasm.", "Winston Churchill"),
        Quote("Success is never owned, it is only rented—and the rent is due every day.", "Rory Vaden"),
        Quote("Success is the sum of a small repeated effort repeated day in and day out.", "Robert Collier"),

        // Productivity & Time (20)
        Quote("Time is what we want most, but what we use worst.", "William Penn"),
        Quote("Lost time is never found again.", "Benjamin Franklin"),
        Quote("The key is not to prioritize your schedule, but to schedule your priorities.", "Stephen Covey"),
        Quote("Productivity is never an accident. It is the result of a commitment to excellence.", "Paul J. Meyer"),
        Quote("Amateurs sit and wait for inspiration, the rest of us just get up and go to work.", "Stephen King"),
        Quote("The difference between successful people and really successful people is that really successful people say no to almost everything.", "Warren Buffett"),
        Quote("Efficiency is doing things right; effectiveness is doing the right things.", "Peter Drucker"),
        Quote("You don't need more time, you need fewer distractions.", "Unknown"),
        Quote("Start where you are. Use what you have. Do what you can.", "Arthur Ashe"),
        Quote("It's not always that we need to do more but rather that we need to focus on less.", "Nathan W. Morris"),
        Quote("The secret of getting ahead is getting started. The secret of getting started is breaking your complex tasks into small tasks.", "Mark Twain"),
        Quote("Until we can manage time, we can manage nothing else.", "Peter Drucker"),
        Quote("Either you run the day or the day runs you.", "Jim Rohn"),
        Quote("Your future is created by what you do today, not tomorrow.", "Robert Kiyosaki"),
        Quote("The bad news is time flies. The good news is you're the pilot.", "Michael Altshuler"),
        Quote("Time is the most valuable thing a man can spend.", "Theophrastus"),
        Quote("The two most powerful warriors are patience and time.", "Leo Tolstoy"),
        Quote("Time management is life management.", "Unknown"),
        Quote("Your time is limited, so don't waste it living someone else's life.", "Steve Jobs"),
        Quote("The greatest resource we have is time.", "Unknown"),

        // Leadership & Influence (20)
        Quote("The greatest leader is not the one who does the greatest things, but the one who gets people to do the greatest things.", "Ronald Reagan"),
        Quote("A leader is one who knows the way, goes the way, and shows the way.", "John C. Maxwell"),
        Quote("The ultimate measure of a man is not where he stands in moments of comfort, but where he stands at times of challenge.", "Martin Luther King Jr."),
        Quote("Before you are a leader, success is all about growing yourself. When you become a leader, success is all about growing others.", "Jack Welch"),
        Quote("The greatest leader is not the one who does the most things, but the one who inspires others to do great things.", "Unknown"),
        Quote("Management is doing things right; leadership is doing the right things.", "Peter Drucker"),
        Quote("A good leader leads by example. A great leader leads by heart.", "Unknown"),
        Quote("People don't care how much you know until they know how much you care.", "Theodore Roosevelt"),
        Quote("The strength of the team is each individual member. The strength of each member is the team.", "Phil Jackson"),
        Quote("Leadership is the art of getting someone else to do something you want done because he wants to do it.", "Dwight D. Eisenhower"),
        Quote("Leadership is not about being in charge. It is about taking care of those in your charge.", "Simon Sinek"),
        Quote("The task of leadership is not to put greatness into people, but to elicit it, for the greatness is there already.", "John Buchan"),
        Quote("A leader is best when people barely know he exists, when his work is done, his aim fulfilled, they will say: we did it ourselves.", "Lao Tzu"),
        Quote("The measure of intelligence is the ability to change.", "Albert Einstein"),
        Quote("Leadership and learning are indispensable to each other.", "John F. Kennedy"),
        Quote("The most dangerous leadership myth is that leaders are born—that there is a genetic factor to leadership.", "Warren Bennis"),
        Quote("He who has never learned to obey cannot be a good commander.", "Aristotle"),
        Quote("The price of greatness is responsibility.", "Winston Churchill"),
        Quote("Leadership is the capacity to translate vision into reality.", "Warren Bennis"),
        Quote("True leadership lies in guiding others to success.", "Unknown"),

        // Creativity & Innovation (20)
        Quote("Creativity is intelligence having fun.", "Albert Einstein"),
        Quote("The chief enemy of creativity is good sense.", "Pablo Picasso"),
        Quote("Logic will get you from A to B. Imagination will take you everywhere.", "Albert Einstein"),
        Quote("Creativity takes courage.", "Henri Matisse"),
        Quote("The creative adult is the child who survived.", "Ursula K. Le Guin"),
        Quote("You can't use up creativity. The more you use, the more you have.", "Maya Angelou"),
        Quote("The desire to create is one of the deepest yearnings of the human soul.", "Dieter F. Uchtdorf"),
        Quote("Imagination is the beginning of creation.", "George Bernard Shaw"),
        Quote("Creativity is thinking up new things. Innovation is doing new things.", "Theodore Levitt"),
        Quote("The most creative people have this childlike trait that is still a part of them.", "Unknown"),
        Quote("Don't think. Thinking is the enemy of creativity.", "Ray Bradbury"),
        Quote("Creativity involves breaking out of established patterns in order to look at things in a different way.", "Edward de Bono"),
        Quote("The true sign of intelligence is not knowledge but imagination.", "Albert Einstein"),
        Quote("To create, you must first destroy. Every act of creation is first an act of destruction.", "Pablo Picasso"),
        Quote("If you're not prepared to be wrong, you'll never come up with anything original.", "Ken Robinson"),
        Quote("Creativity is the residue of time wasted.", "Albert Einstein"),
        Quote("The creative adult is the child who survived.", "Ursula K. Le Guin"),
        Quote("You can't use up creativity. The more you use, the more you have.", "Maya Angelou"),
        Quote("The desire to create is one of the deepest yearnings of the human soul.", "Dieter F. Uchtdorf"),
        Quote("Imagination is the beginning of creation.", "George Bernard Shaw"),

        // Patience & Consistency (20)
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
        Quote("We are what we repeatedly do. Excellence is not an act but a habit.", "Will Durant"),
        Quote("Patience is bitter, but its fruit is sweet.", "Jean-Jacques Rousseau"),
        Quote("The secret of patience is to do something else in the meantime.", "Unknown"),
        Quote("Consistency is the key to success in any endeavor.", "Unknown"),
        Quote("The difference between a master and a beginner is that the master has failed more times than the beginner has tried.", "Unknown"),
        Quote("Consistency is more important than perfection.", "Unknown"),

        // Gratitude & Positivity (20)
        Quote("Gratitude turns what we have into enough.", "Melody Beattie"),
        Quote("The more grateful I am, the more beauty I see.", "Mary Davis"),
        Quote("Joy is the simplest form of gratitude.", "Karl Barth"),
        Quote("Appreciation is a wonderful thing. It makes what is excellent in others belong to us as well.", "Voltaire"),
        Quote("When you are grateful, fear disappears and abundance appears.", "Tony Robbins"),
        Quote("Let us be grateful to the people who make us happy.", "Marcel Proust"),
        Quote("Gratitude is not only the greatest of virtues, but the parent of all others.", "Cicero"),
        Quote("Piglet noticed that even though he had a very small heart, it could hold a rather large amount of gratitude.", "A.A. Milne"),
        Quote("No duty is more urgent than that of returning thanks.", "James Allen"),
        Quote("Gratitude changes the pangs of memory into a tranquil joy.", "Dietrich Bonhoeffer"),
        Quote("When I started counting my blessings, my whole life turned around.", "Willie Nelson"),
        Quote("Feeling gratitude and not expressing it is like wrapping a present and not giving it.", "William Arthur Ward"),
        Quote("The soul that gives thanks can find comfort in everything.", "Hannah Whitall Smith"),
        Quote("Enjoy the little things, for one day you may look back and realize they were the big things.", "Robert Brault"),
        Quote("Happiness cannot be traveled to, owned, earned, worn, or consumed. Happiness is the spiritual experience of living every minute with love, grace, and gratitude.", "Denis Waitley"),
        Quote("Gratitude is the fairest blossom which springs from the soul.", "Henry Ward Beecher"),
        Quote("When you are grateful, you are great.", "Unknown"),
        Quote("Gratitude is the sign of noble souls.", "Aesop"),
        Quote("We can always find something to be thankful for.", "Unknown"),
        Quote("Be thankful for what you have; you'll end up having more.", "Oprah Winfrey"),

        // Failure & Setbacks (20)
        Quote("I have not failed. I've just found 10,000 ways that won't work.", "Thomas Edison"),
        Quote("Failure is not the opposite of success: it's part of success.", "Arianna Huffington"),
        Quote("Every adversity, every failure, every heartache carries with it the seed of an equal or greater benefit.", "Napoleon Hill"),
        Quote("Failure is another stepping stone to greatness.", "Oprah Winfrey"),
        Quote("There is no innovation and creativity without failure.", "Brené Brown"),
        Quote("The only real failure in life is not to be true to the best one knows.", "Buddha"),
        Quote("If you're not failing, you're not pushing your limits, and if you're not pushing your limits, you're not maximizing your potential.", "Trevor Noah"),
        Quote("Failure is the tuition you pay for success.", "Walter Brunell"),
        Quote("Success is stumbling from failure to failure with no loss of enthusiasm.", "Winston Churchill"),
        Quote("A man's character is his fate.", "Heraclitus"),
        Quote("It is impossible to live without failing at something, unless you live so cautiously that you might as well not have lived at all.", "J.K. Rowling"),
        Quote("Failure isn't fatal, but failure to change might be.", "John Wooden"),
        Quote("You don't learn to walk by following rules. You learn by doing, and by falling over.", "Richard Branson"),
        Quote("Rock bottom became the solid foundation on which I rebuilt my life.", "J.K. Rowling"),
        Quote("There is only one thing that makes a dream impossible to achieve: the fear of failure.", "Paulo Coelho"),
        Quote("Failure is the condiment that gives success its flavor.", "Truman Capote"),
        Quote("I can accept failure, everyone fails at something. But I can't accept not trying.", "Michael Jordan"),
        Quote("Failures are the stepping stones to success.", "Unknown"),
        Quote("Every failure is a lesson learned.", "Unknown"),
        Quote("Failure is not falling down but refusing to get up.", "Chinese Proverb"),

        // Inner Strength & Self-Belief (20)
        Quote("No one can make you feel inferior without your consent.", "Eleanor Roosevelt"),
        Quote("The most terrifying thing is to accept oneself completely.", "Carl Jung"),
        Quote("You yourself, as much as anybody in the entire universe, deserve your love and affection.", "Buddha"),
        Quote("To be yourself in a world that is constantly trying to make you something else is the greatest accomplishment.", "Ralph Waldo Emerson"),
        Quote("The only person you are destined to become is the person you decide to be.", "Ralph Waldo Emerson"),
        Quote("Be who you are and say what you feel, because those who mind don't matter, and those who matter don't mind.", "Dr. Seuss"),
        Quote("Believe in yourself and all that you are. Know that there is something inside you that is greater than any obstacle.", "Christian D. Larson"),
        Quote("Self-confidence is the first requisite to great undertakings.", "Samuel Johnson"),
        Quote("You have been criticizing yourself for years, and it hasn't worked. Try approving of yourself and see what happens.", "Louise Hay"),
        Quote("The eyes are the mirror of the soul and reflect everything that seems to be hidden; and they reflect everything one doesn't see.", "Paulo Coelho"),
        Quote("If you hear a voice within you say you cannot paint, then by all means paint, and that voice will be silenced.", "Vincent van Gogh"),
        Quote("Trust yourself. You know more than you think you do.", "Benjamin Spock"),
        Quote("Our greatest weakness lies in giving up. The most certain way to try again is to try one more time.", "Thomas Edison"),
        Quote("Low self-esteem is like driving through life with your hand-break on.", "Maxwell Maltz"),
        Quote("Confidence comes from discipline and training.", "Robert Kiyosaki"),
        Quote("Believe you can and you're halfway there.", "Theodore Roosevelt"),
        Quote("You are enough just as you are.", "Meghan Markle"),
        Quote("Self-belief is not a luxury; it is a necessity.", "Unknown"),
        Quote("The moment you doubt whether you can fly, you cease forever to be able to do it.", "J.M. Barrie"),
        Quote("Your belief determines your action and your action determines your results.", "Unknown"),

        // Balance & Life (20)
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
        Quote("If you look at what you have in life, you'll always have more.", "Oprah Winfrey"),
        Quote("Life is not measured by the number of breaths we take, but by the moments that take our breath away.", "Maya Angelou"),
        Quote("Spread love everywhere you go. Let no one ever come to you without leaving happier.", "Mother Teresa"),
        Quote("Always remember that you are absolutely unique. Just like everyone else.", "Margaret Mead"),
        Quote("The purpose of life is a life of purpose.", "Robert Byrne"),
        Quote("Life is not about waiting for the storm to pass, but learning to dance in the rain.", "Unknown"),
        Quote("In three words I can sum up everything I've learned about life: it goes on.", "Robert Frost"),
        Quote("Life is a succession of lessons which must be lived to be understood.", "Ralph Waldo Emerson"),
        Quote("The biggest adventure you can take is to live the life of your dreams.", "Oprah Winfrey"),

        // Change & Adaptability (20)
        Quote("Be the change that you wish to see in the world.", "Mahatma Gandhi"),
        Quote("Change is the law of life. And those who look only to the past or present are certain to miss the future.", "John F. Kennedy"),
        Quote("If you don't like something, change it. If you can't change it, change your attitude.", "Maya Angelou"),
        Quote("The secret of change is to focus all of your energy, not on fighting the old, but on building the new.", "Socrates"),
        Quote("Progress is impossible without change, and those who cannot change their minds cannot change anything.", "George Bernard Shaw"),
        Quote("Change your thoughts and you change your world.", "Norman Vincent Peale"),
        Quote("The measure of intelligence is the ability to change.", "Albert Einstein"),
        Quote("They always say time changes things, but you actually have to change them yourself.", "Andy Warhol"),
        Quote("Every moment is a fresh beginning.", "T.S. Eliot"),
        Quote("The only way to make sense out of change is to plunge into it, move with it, and join the dance.", "Alan Watts"),
        Quote("Change is never painful, only the resistance to change is painful.", "Buddha"),
        Quote("What the new year brings to you will depend a great deal on what you bring to the year.", "Vern McLellan"),
        Quote("New beginnings are often disguised as painful endings.", "Lao Tzu"),
        Quote("You can't build a reputation on what you're going to do.", "Henry Ford"),
        Quote("I am not what happened to me. I am what I choose to become.", "Carl Jung"),
        Quote("The measure of a man is what he does with power.", "Plato"),
        Quote("If we don't change, we don't grow. If we don't grow, we aren't really living.", "Gail Sheehy"),
        Quote("Yesterday I was clever, so I wanted to change the world. Today I am wise, so I am changing myself.", "Rumi"),
        Quote("Progress is impossible without change.", "George Bernard Shaw"),
        Quote("The only way to make sense out of change is to plunge into it.", "Alan Watts")
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
        val json = prefs?.getString("shown_quotes", null) ?: return
        try {
            val obj = JSONObject(json)
            val timestamp = obj.optLong("timestamp", 0L)
            val now = System.currentTimeMillis()
            if (now - timestamp < 24 * 60 * 60 * 1000L) {
                val arr = obj.optJSONArray("quotes") ?: return
                shownQuotes.clear()
                for (i in 0 until arr.length()) {
                    shownQuotes.add(arr.getString(i))
                }
            } else {
                shownQuotes.clear()
                prefs?.edit()?.remove("shown_quotes")?.apply()
            }
        } catch (_: Exception) {
            shownQuotes.clear()
        }
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

    suspend fun getRandomQuote(): Quote {
        if (quoteQueue.isEmpty()) refillQueue()
        val quote = quoteQueue.removeFirstOrNull() ?: offlineQuotes.random()
        shownQuotes.add(quote.text)
        if (shownQuotes.size > 100) {
            val iter = shownQuotes.iterator()
            repeat(50) { if (iter.hasNext()) iter.next(); iter.remove() }
        }
        saveShownQuotes()
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
        val quote = if (filtered.isNotEmpty()) filtered.random() else all.random()
        shownQuotes.add(quote.text)
        saveShownQuotes()
        return quote
    }

    suspend fun getTimedQuote(): Quote {
        val timeOfDay = getTimeOfDay()
        val timeKeywords = when (timeOfDay) {
            "morning" -> listOf("morning", "day", "begin", "start", "wake", "dawn")
            "afternoon" -> listOf("work", "focus", "productivity", "progress", "momentum")
            "evening" -> listOf("rest", "reflect", "peace", "calm", "gratitude", "sleep")
            else -> emptyList()
        }
        val all = (offlineQuotes + onlineQuotes).filter { q ->
            timeKeywords.any { kw -> q.text.contains(kw, ignoreCase = true) }
        }
        val filtered = all.filter { it.text !in shownQuotes }
        val quote = if (filtered.isNotEmpty()) filtered.random() else getRandomQuote()
        shownQuotes.add(quote.text)
        saveShownQuotes()
        return quote
    }

    fun isApiEnabled(): Boolean {
        return prefs?.getBoolean(KEY_API_ENABLED, false) ?: false
    }

    fun setApiEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_API_ENABLED, enabled)?.apply()
    }

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
