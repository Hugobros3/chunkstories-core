const vec4 grad4[32] = {
    vec4(0.0, 1.0, 1.0, 1.0),
    vec4(0.0, 1.0, 1.0, -1.0),
    vec4(0.0, 1.0, -1.0, 1.0),
    vec4(0.0, 1.0, -1.0, -1.0),
    vec4(0.0, -1.0, 1.0, 1.0),
    vec4(0.0, -1.0, 1.0, -1.0),
    vec4(0.0, -1.0, -1.0, 1.0),
    vec4(0.0, -1.0, -1.0, -1.0),

    vec4(1.0, 0.0, 1.0, 1.0),
    vec4(1.0, 0.0, 1.0, -1.0),
    vec4(1.0, 0.0, -1.0, 1.0),
    vec4(1.0, 0.0, -1.0, -1.0),
    vec4(-1.0, 0.0, 1.0, 1.0),
    vec4(-1.0, 0.0, 1.0, -1.0),
    vec4(-1.0, 0.0, -1.0, 1.0),
    vec4(-1.0, 0.0, -1.0, -1.0),

    vec4(1.0, 1.0, 0.0, 1.0),
    vec4(1.0, 1.0, 0.0, -1.0),
    vec4(1.0, -1.0, 0.0, 1.0),
    vec4(1.0, -1.0, 0.0, -1.0),
    vec4(-1.0, 1.0, 0.0, 1.0),
    vec4(-1.0, 1.0, 0.0, -1.0),
    vec4(-1.0, -1.0, 0.0, 1.0),
    vec4(-1.0, -1.0, 0.0, -1.0),

    vec4(1.0, 1.0, 1.0, 0.0),
    vec4(1.0, 1.0, -1.0, 0.0),
    vec4(1.0, -1.0, 1.0, 0.0),
    vec4(1.0, -1.0, -1.0, 0.0),
    vec4(-1.0, 1.0, 1.0, 0.0),
    vec4(-1.0, 1.0, -1.0, 0.0),
    vec4(-1.0, -1.0, 1.0, 0.0),
    vec4(-1.0, -1.0, -1.0, 0.0)
};

const float F4 = 0.309017;
const float G4 = 0.1381966;
//const float F4 = ((sqrt(5.0) - 1.0) / 4.0);
//const float G4 = ((5.0 - sqrt(5.0)) / 20.0);

int fastfloor(float x) {
    int xi = int(floor(x));
    return x < xi ? xi - 1 : xi;
}

float noise4d(/*PrecomputedSimplexSeed seed, */float x, float y, float z, float w) {
    float n0, n1, n2, n3, n4; 
    // Noise contributions from the five corners
    // Skew the (x,y,z,w) space to determine which cell of 24 simplices
    // we're in
    float s = (x + y + z + w) * F4; // Factor for 4D skewing
    int i = fastfloor(x + s);
    int j = fastfloor(y + s);
    int k = fastfloor(z + s);
    int l = fastfloor(w + s);
    float t = (i + j + k + l) * G4; // Factor for 4D unskewing
    float X0 = i - t; // Unskew the cell origin back to (x,y,z,w) space
    float Y0 = j - t;
    float Z0 = k - t;
    float W0 = l - t;
    float x0 = x - X0; // The x,y,z,w distances from the cell origin
    float y0 = y - Y0;
    float z0 = z - Z0;
    float w0 = w - W0;
    // For the 4D case, the simplex is a 4D shape I won't even try to
    // describe.
    // To find out which of the 24 possible simplices we're in, we need to
    // determine the magnitude ordering of x0, y0, z0 and w0.
    // Six pair-wise comparisons are performed between each possible pair
    // of the four coordinates, and the results are used to rank the
    // numbers.
    int rankx = 0;
    int ranky = 0;
    int rankz = 0;
    int rankw = 0;
    if (x0 > y0)
        rankx++;
    else
        ranky++;
    if (x0 > z0)
        rankx++;
    else
        rankz++;
    if (x0 > w0)
        rankx++;
    else
        rankw++;
    if (y0 > z0)
        ranky++;
    else
        rankz++;
    if (y0 > w0)
        ranky++;
    else
        rankw++;
    if (z0 > w0)
        rankz++;
    else
        rankw++;
    int i1, j1, k1, l1; // The integer offsets for the second simplex corner
    int i2, j2, k2, l2; // The integer offsets for the third simplex corner
    int i3, j3, k3, l3; // The integer offsets for the fourth simplex corner
    // simplex[c] is a 4-vector with the numbers 0, 1, 2 and 3 in some
    // order.
    // Many values of c will never occur, since e.g. x>y>z>w makes x<z, y<w
    // and x<w
    // impossible. Only the 24 indices which have non-zero entries make any
    // sense.
    // We use a thresholding to set the coordinates in turn from the largest
    // magnitude.
    // Rank 3 denotes the largest coordinate.
    i1 = rankx >= 3 ? 1 : 0;
    j1 = ranky >= 3 ? 1 : 0;
    k1 = rankz >= 3 ? 1 : 0;
    l1 = rankw >= 3 ? 1 : 0;
    // Rank 2 denotes the second largest coordinate.
    i2 = rankx >= 2 ? 1 : 0;
    j2 = ranky >= 2 ? 1 : 0;
    k2 = rankz >= 2 ? 1 : 0;
    l2 = rankw >= 2 ? 1 : 0;
    // Rank 1 denotes the second smallest coordinate.
    i3 = rankx >= 1 ? 1 : 0;
    j3 = ranky >= 1 ? 1 : 0;
    k3 = rankz >= 1 ? 1 : 0;
    l3 = rankw >= 1 ? 1 : 0;
    // The fifth corner has all coordinate offsets = 1, so no need to
    // compute that.
    float x1 = x0 - i1 + G4; // Offsets for second corner in (x,y,z,w) // coords
    float y1 = y0 - j1 + G4;
    float z1 = z0 - k1 + G4;
    float w1 = w0 - l1 + G4;
    float x2 = x0 - i2 + 2.0f * G4; // Offsets for third corner in (x,y,z,w)  // coords
    float y2 = y0 - j2 + 2.0f * G4;
    float z2 = z0 - k2 + 2.0f * G4;
    float w2 = w0 - l2 + 2.0f * G4;
    float x3 = x0 - i3 + 3.0f * G4; // Offsets for fourth corner in (x,y,z,w) coords
    float y3 = y0 - j3 + 3.0f * G4;
    float z3 = z0 - k3 + 3.0f * G4;
    float w3 = w0 - l3 + 3.0f * G4;
    float x4 = x0 - 1.0f + 4.0f * G4; // Offsets for last corner in (x,y,z,w) coords
    float y4 = y0 - 1.0f + 4.0f * G4;
    float z4 = z0 - 1.0f + 4.0f * G4;
    float w4 = w0 - 1.0f + 4.0f * G4;
    // Work out the hashed gradient indices of the five simplex corners
    int ii = i & 255;
    int jj = j & 255;
    int kk = k & 255;
    int ll = l & 255;
    int gi0 = simplexSeed.perm[ii + simplexSeed.perm[jj + simplexSeed.perm[kk + simplexSeed.perm[ll]]]] % 32;
    int gi1 = simplexSeed.perm[ii + i1 + simplexSeed.perm[jj + j1 + simplexSeed.perm[kk + k1 + simplexSeed.perm[ll + l1]]]] % 32;
    int gi2 = simplexSeed.perm[ii + i2 + simplexSeed.perm[jj + j2 + simplexSeed.perm[kk + k2 + simplexSeed.perm[ll + l2]]]] % 32;
    int gi3 = simplexSeed.perm[ii + i3 + simplexSeed.perm[jj + j3 + simplexSeed.perm[kk + k3 + simplexSeed.perm[ll + l3]]]] % 32;
    int gi4 = simplexSeed.perm[ii + 1 + simplexSeed.perm[jj + 1 + simplexSeed.perm[kk + 1 + simplexSeed.perm[ll + 1]]]] % 32;
    // Calculate the contribution from the five corners
    float t0 = 0.6f - x0 * x0 - y0 * y0 - z0 * z0 - w0 * w0;
    if (t0 < 0)
        n0 = 0.0f;
    else {
        t0 *= t0;
        n0 = t0 * t0 * dot(grad4[gi0], vec4(x0, y0, z0, w0));
    }
    float t1 = 0.6f - x1 * x1 - y1 * y1 - z1 * z1 - w1 * w1;
    if (t1 < 0)
        n1 = 0.0f;
    else {
        t1 *= t1;
        n1 = t1 * t1 * dot(grad4[gi1], vec4(x1, y1, z1, w1));
    }
    float t2 = 0.6f - x2 * x2 - y2 * y2 - z2 * z2 - w2 * w2;
    if (t2 < 0.0f)
        n2 = 0.0f;
    else {
        t2 *= t2;
        n2 = t2 * t2 * dot(grad4[gi2], vec4(x2, y2, z2, w2));
    }
    float t3 = 0.6f - (x3 * x3) - (y3 * y3) - z3 * z3 - (w3 * w3);
    if (t3 < 0.0f)
        n3 = 0.0f;
    else {
        t3 *= t3;
        n3 = t3 * t3 * dot(grad4[gi3], vec4(x3, y3, z3, w3));
    }
    float t4 = 0.6f - (x4 * x4) - (y4 * y4) - (z4 * z4) - (w4 * w4);
    if (t4 < 0.0f)
        n4 = 0.0f;
    else {
        t4 *= t4;
        n4 = t4 * t4 * dot(grad4[gi4], vec4(x4, y4, z4, w4));
    }
    // Sum up and scale the result to cover the range [-1,1]
    return 27.0f * (n0 + n1 + n2 + n3 + n4);
}

float noise4d(vec4 vec) {
    return noise4d(vec.x, vec.y, vec.z, vec.w);
}
